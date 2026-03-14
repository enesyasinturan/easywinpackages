package com.softeyt.easywinpackages.viewmodel;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.softeyt.easywinpackages.i18n.MessageService;
import com.softeyt.easywinpackages.service.PackageManagerService;
import com.softeyt.easywinpackages.service.impl.ChocoService;
import com.softeyt.easywinpackages.service.impl.ScoopService;
import com.softeyt.easywinpackages.service.impl.WingetService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


@Singleton
public class SourcesViewModel {

    
    public enum SourceOperation {
        IDLE,
        CHECKING,
        INSTALLING,
        REMOVING
    }

    private final WingetService wingetService;
    private final ChocoService  chocoService;
    private final ScoopService  scoopService;
    private final MessageService messageService;

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "sources-worker");
        t.setDaemon(true);
        return t;
    });

    private final BooleanProperty wingetAvailable  = new SimpleBooleanProperty(false);
    private final BooleanProperty chocoAvailable   = new SimpleBooleanProperty(false);
    private final BooleanProperty scoopAvailable   = new SimpleBooleanProperty(false);
    private final BooleanProperty wingetChecking   = new SimpleBooleanProperty(false);
    private final BooleanProperty chocoChecking    = new SimpleBooleanProperty(false);
    private final BooleanProperty scoopChecking    = new SimpleBooleanProperty(false);
    private final ObjectProperty<SourceOperation> wingetOperation = new SimpleObjectProperty<>(SourceOperation.IDLE);
    private final ObjectProperty<SourceOperation> chocoOperation = new SimpleObjectProperty<>(SourceOperation.IDLE);
    private final ObjectProperty<SourceOperation> scoopOperation = new SimpleObjectProperty<>(SourceOperation.IDLE);

    @Inject
    public SourcesViewModel(WingetService wingetService,
                             ChocoService chocoService,
                             ScoopService scoopService,
                             MessageService messageService) {
        this.wingetService = wingetService;
        this.chocoService  = chocoService;
        this.scoopService  = scoopService;
        this.messageService = messageService;
    }

    
    public void checkAll() {
        checkSource(wingetService, wingetAvailable, wingetChecking, wingetOperation);
        checkSource(chocoService,  chocoAvailable,  chocoChecking, chocoOperation);
        checkSource(scoopService,  scoopAvailable,  scoopChecking, scoopOperation);
    }

    
    public void installWinget(Runnable onSuccess, Consumer<String> onError) {
        runInstall(wingetService, wingetAvailable, wingetChecking, wingetOperation, onSuccess, onError);
    }

    
    public void installChoco(Runnable onSuccess, Consumer<String> onError) {
        runInstall(chocoService, chocoAvailable, chocoChecking, chocoOperation, onSuccess, onError);
    }

    
    public void installScoop(Runnable onSuccess, Consumer<String> onError) {
        runInstall(scoopService, scoopAvailable, scoopChecking, scoopOperation, onSuccess, onError);
    }

    
    public void removeWinget(Runnable onSuccess, Consumer<String> onError) {
        runRemoval(wingetService, wingetAvailable, wingetChecking, wingetOperation, onSuccess, onError);
    }

    
    public void removeChoco(Runnable onSuccess, Consumer<String> onError) {
        runRemoval(chocoService, chocoAvailable, chocoChecking, chocoOperation, onSuccess, onError);
    }

    
    public void removeScoop(Runnable onSuccess, Consumer<String> onError) {
        runRemoval(scoopService, scoopAvailable, scoopChecking, scoopOperation, onSuccess, onError);
    }

    private void checkSource(PackageManagerService service,
                              BooleanProperty available,
                              BooleanProperty checking,
                              ObjectProperty<SourceOperation> operation) {
        if (operation.get() == SourceOperation.INSTALLING || operation.get() == SourceOperation.REMOVING) {
            return;
        }
        operation.set(SourceOperation.CHECKING);
        checking.set(true);
        executor.submit(() -> {
            boolean result = service.isAvailable();
            Platform.runLater(() -> {
                available.set(result);
                checking.set(false);
                operation.set(SourceOperation.IDLE);
            });
        });
    }

    private void runInstall(PackageManagerService service,
                            BooleanProperty available,
                            BooleanProperty checking,
                            ObjectProperty<SourceOperation> operation,
                            Runnable onSuccess,
                            Consumer<String> onError) {
        if (checking.get()) {
            return;
        }
        operation.set(SourceOperation.INSTALLING);
        checking.set(true);
        Task<Void> task = service.installManager();
        task.setOnSucceeded(e -> executor.submit(() -> {
            boolean isAvailable = waitUntilAvailable(service);
            Platform.runLater(() -> {
                available.set(isAvailable);
                checking.set(false);
                operation.set(SourceOperation.IDLE);
                if (isAvailable) {
                    onSuccess.run();
                    return;
                }
                onError.accept(messageService.getString("sources.install.verify.failed", service.getDisplayName()));
            });
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            checking.set(false);
            operation.set(SourceOperation.IDLE);
            String msg = task.getException() != null
                    ? task.getException().getMessage() : "Installation failed.";
            onError.accept(msg);
        }));
        executor.submit(task);
    }

    private void runRemoval(PackageManagerService service,
                            BooleanProperty available,
                            BooleanProperty checking,
                            ObjectProperty<SourceOperation> operation,
                            Runnable onSuccess,
                            Consumer<String> onError) {
        if (checking.get()) {
            return;
        }
        operation.set(SourceOperation.REMOVING);
        checking.set(true);
        Task<Void> task = service.removeManager();
        task.setOnSucceeded(e -> executor.submit(() -> {
            boolean isAvailable = waitUntilUnavailable(service);
            Platform.runLater(() -> {
                available.set(isAvailable);
                checking.set(false);
                operation.set(SourceOperation.IDLE);
                if (!isAvailable) {
                    onSuccess.run();
                    return;
                }
                onError.accept(messageService.getString("sources.remove.verify.failed", service.getDisplayName()));
            });
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            checking.set(false);
            operation.set(SourceOperation.IDLE);
            String msg = task.getException() != null
                    ? task.getException().getMessage() : "Removal failed.";
            onError.accept(msg);
        }));
        executor.submit(task);
    }

    private boolean waitUntilAvailable(PackageManagerService service) {
        int attempts = service instanceof WingetService ? 12 : 6;
        long delayMillis = service instanceof WingetService ? 2_000L : 1_500L;
        for (int attempt = 0; attempt < attempts; attempt++) {
            if (service.isAvailable()) {
                return true;
            }
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return service.isAvailable();
            }
        }
        return service.isAvailable();
    }

    private boolean waitUntilUnavailable(PackageManagerService service) {
        for (int attempt = 0; attempt < 6; attempt++) {
            if (!service.isAvailable()) {
                return false;
            }
            try {
                Thread.sleep(1_500L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return service.isAvailable();
            }
        }
        return service.isAvailable();
    }

    public BooleanProperty wingetAvailableProperty()  { return wingetAvailable; }
    public BooleanProperty chocoAvailableProperty()   { return chocoAvailable; }
    public BooleanProperty scoopAvailableProperty()   { return scoopAvailable; }
    public BooleanProperty wingetCheckingProperty()   { return wingetChecking; }
    public BooleanProperty chocoCheckingProperty()    { return chocoChecking; }
    public BooleanProperty scoopCheckingProperty()    { return scoopChecking; }
    public ReadOnlyObjectProperty<SourceOperation> wingetOperationProperty() { return wingetOperation; }
    public ReadOnlyObjectProperty<SourceOperation> chocoOperationProperty()  { return chocoOperation; }
    public ReadOnlyObjectProperty<SourceOperation> scoopOperationProperty()  { return scoopOperation; }
}

