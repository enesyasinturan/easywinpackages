package com.softeyt.easywinpackages.viewmodel;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.softeyt.easywinpackages.i18n.MessageService;
import com.softeyt.easywinpackages.model.Package;
import com.softeyt.easywinpackages.model.PackageSource;
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
public class UninstallViewModel {

    private static final Logger log = LoggerFactory.getLogger(UninstallViewModel.class);
    private static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final WingetService wingetService;
    private final ChocoService  chocoService;
    private final ScoopService  scoopService;
    private final MessageService messageService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "uninstall-worker");
        t.setDaemon(true);
        return t;
    });

    private final ObservableList<Package> packages = FXCollections.observableArrayList();
    private final StringProperty  statusMessage = new SimpleStringProperty("");
    private final BooleanProperty busy          = new SimpleBooleanProperty(false);
    private final StringProperty  operationLog  = new SimpleStringProperty("");

    @Inject
    public UninstallViewModel(WingetService wingetService,
                               ChocoService chocoService,
                               ScoopService scoopService,
                               MessageService messageService) {
        this.wingetService = wingetService;
        this.chocoService  = chocoService;
        this.scoopService  = scoopService;
        this.messageService = messageService;
    }

    
    public void loadInstalled(PackageSource filter) {
        busy.set(true);
        packages.clear();
        statusMessage.set("");
        appendLog("uninstall.log.load.start", filter == null
                ? messageService.getString("common.source.all.short")
                : filter.name());

        executor.submit(() -> {
            List<Package> all = new ArrayList<>();
            if (filter == null || filter.name().equals("ALL")) {
                collectInstalled(wingetService, all);
                collectInstalled(chocoService, all);
                collectInstalled(scoopService, all);
            } else {
                collectInstalled(resolveService(filter), all);
            }
            Platform.runLater(() -> {
                packages.setAll(all);
                appendLog("uninstall.log.load.success", all.size());
                busy.set(false);
            });
        });
    }

    
    public void uninstall(Package pkg, Runnable onSuccess, Consumer<String> onError) {
        if (pkg == null) return;
        busy.set(true);
        appendLog("uninstall.log.start", pkg.name(), pkg.source().name());
        Task<Void> task = resolveService(pkg.source()).uninstall(pkg);
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            busy.set(false);
            packages.remove(pkg);
            appendLog("uninstall.log.success", pkg.name());
            onSuccess.run();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            busy.set(false);
            String message = task.getException() != null ? task.getException().getMessage() : "Unknown uninstall error";
            appendLog("uninstall.log.error", message);
            onError.accept(message);
        }));
        executor.submit(task);
    }

    private void collectInstalled(PackageManagerService service, List<Package> sink) {
        if (service == null || !service.isAvailable()) return;
        try {
            Task<List<Package>> t = service.getInstalled();
            t.run();
            sink.addAll(t.get());
        } catch (Exception e) {
            appendLog("uninstall.log.error", e.getMessage());
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

    public ObservableList<Package> getPackages()  { return packages; }
    public StringProperty  statusMessageProperty() { return statusMessage; }
    public BooleanProperty busyProperty()          { return busy; }
    public StringProperty  operationLogProperty()  { return operationLog; }
}

