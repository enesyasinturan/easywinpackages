package com.softeyt.easywinpackages.viewmodel;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.softeyt.easywinpackages.i18n.MessageService;
import com.softeyt.easywinpackages.model.PackageSource;
import com.softeyt.easywinpackages.model.UpdateInfo;
import com.softeyt.easywinpackages.service.PackageManagerService;
import com.softeyt.easywinpackages.service.impl.ChocoService;
import com.softeyt.easywinpackages.service.impl.ScoopService;
import com.softeyt.easywinpackages.service.impl.WingetService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


@Singleton
public class UpdatesViewModel {

    private static final Logger log = LoggerFactory.getLogger(UpdatesViewModel.class);
    private static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final WingetService wingetService;
    private final ChocoService  chocoService;
    private final ScoopService  scoopService;
    private final MessageService messageService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "updates-worker");
        t.setDaemon(true);
        return t;
    });

    private final ObservableList<UpdateInfo> updates = FXCollections.observableArrayList();
    private final StringProperty  statusMessage = new SimpleStringProperty("");
    private final BooleanProperty busy          = new SimpleBooleanProperty(false);
    private final StringProperty  operationLog  = new SimpleStringProperty("");

    @Inject
    public UpdatesViewModel(WingetService wingetService,
                             ChocoService chocoService,
                             ScoopService scoopService,
                             MessageService messageService) {
        this.wingetService = wingetService;
        this.chocoService  = chocoService;
        this.scoopService  = scoopService;
        this.messageService = messageService;
    }

    
    public void loadUpdates() {
        busy.set(true);
        updates.clear();
        statusMessage.set("");
        appendLog("updates.log.load.start");

        executor.submit(() -> {
            List<UpdateInfo> all = new ArrayList<>();
            collectUpdates(wingetService, all);
            collectUpdates(chocoService, all);
            collectUpdates(scoopService, all);

            Platform.runLater(() -> {
                updates.setAll(all);
                if (all.isEmpty()) statusMessage.set(messageService.getString("updates.no.updates"));
                appendLog("updates.log.load.success", all.size());
                busy.set(false);
            });
        });
    }

    
    public void update(UpdateInfo info, Runnable onSuccess, Consumer<String> onError) {
        if (info == null) return;
        busy.set(true);
        appendLog("updates.log.update.start", info.name(), info.source().name());
        Task<Void> task = resolveService(info.source()).update(info);
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            appendLog("updates.log.update.success", info.name());
            busy.set(false);
            loadUpdates();
            onSuccess.run();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            busy.set(false);
            String message = task.getException() != null ? task.getException().getMessage() : "Unknown update error";
            appendLog("updates.log.error", message);
            onError.accept(message);
        }));
        executor.submit(task);
    }

    
    public void updateAll(Runnable onSuccess, Consumer<String> onError) {
        busy.set(true);
        appendLog("updates.log.update.all.start");
        executor.submit(() -> {
            try {
                if (wingetService.isAvailable()) { Task<Void> t = wingetService.updateAll(); t.run(); t.get(); }
                if (chocoService.isAvailable())  { Task<Void> t = chocoService.updateAll();  t.run(); t.get(); }
                if (scoopService.isAvailable())  { Task<Void> t = scoopService.updateAll();  t.run(); t.get(); }
                Platform.runLater(() -> {
                    appendLog("updates.log.update.all.success");
                    busy.set(false);
                    loadUpdates();
                    onSuccess.run();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    busy.set(false);
                    String message = e.getMessage() != null ? e.getMessage() : "Unknown update error";
                    appendLog("updates.log.error", message);
                    onError.accept(message);
                });
            }
        });
    }

    private void collectUpdates(PackageManagerService service, List<UpdateInfo> sink) {
        if (!service.isAvailable()) return;
        try {
            Task<List<UpdateInfo>> t = service.getUpdates();
            t.run();
            sink.addAll(t.get());
        } catch (Exception e) {
            appendLog("updates.log.load.error", service.getDisplayName(), e.getMessage());
        }
    }

    private void appendLog(String key, Object... args) {
        appendLogRaw(messageService.getString(key, args));
    }

    private void appendLogRaw(String message) {
        log.info(message);
        String logEntry = "[" + LocalTime.now().format(LOG_TIME_FORMATTER) + "] " + message;
        runOnFxThread(() -> operationLog.set(operationLog.get().isBlank()
                ? logEntry
                : operationLog.get() + "\n" + logEntry));
    }

    private void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        Platform.runLater(action);
    }

    private PackageManagerService resolveService(PackageSource source) {
        return switch (source) {
            case WINGET -> wingetService;
            case CHOCO  -> chocoService;
            case SCOOP  -> scoopService;
        };
    }

    public ObservableList<UpdateInfo> getUpdates()  { return updates; }
    public StringProperty  statusMessageProperty()   { return statusMessage; }
    public BooleanProperty busyProperty()            { return busy; }
    public StringProperty  operationLogProperty()    { return operationLog; }
}

