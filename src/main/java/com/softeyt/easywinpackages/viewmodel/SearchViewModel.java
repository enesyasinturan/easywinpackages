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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Singleton
public class SearchViewModel {

    private static final Logger log = LoggerFactory.getLogger(SearchViewModel.class);
    private static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final WingetService wingetService;
    private final ChocoService  chocoService;
    private final ScoopService  scoopService;
    private final MessageService messageService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "search-worker");
        t.setDaemon(true);
        return t;
    });

    private final ObservableList<Package> results = FXCollections.observableArrayList();
    private final StringProperty  statusMessage = new SimpleStringProperty("");
    private final BooleanProperty busy          = new SimpleBooleanProperty(false);
    private final StringProperty  operationLog  = new SimpleStringProperty("");
    private final ReadOnlyIntegerWrapper stateRevision = new ReadOnlyIntegerWrapper(0);
    private final Set<String> installedPackageKeys = ConcurrentHashMap.newKeySet();
    private final Set<String> installingPackageKeys = ConcurrentHashMap.newKeySet();

    @Inject
    public SearchViewModel(WingetService wingetService,
                            ChocoService chocoService,
                            ScoopService scoopService,
                            MessageService messageService) {
        this.wingetService = wingetService;
        this.chocoService  = chocoService;
        this.scoopService  = scoopService;
        this.messageService = messageService;
    }

    
    public void search(String query, PackageSource source) {
        if (query == null || query.isBlank()) return;
        if (source == null) {
            searchAll(query);
            return;
        }
        busy.set(true);
        results.clear();
        statusMessage.set("");
        resetPackageState();
        clearOperationLog();
        appendLog("search.log.search.start", query, source.name());

        PackageManagerService service = resolveService(source);
        if (service == null || !service.isAvailable()) {
            String message = messageService.getString("search.status.source.unavailable", source.name());
            statusMessage.set(message);
            appendLog("search.log.search.error", message);
            busy.set(false);
            return;
        }

        Task<List<Package>> task = service.search(query);
        task.setOnSucceeded(e -> executor.submit(() -> finalizeSearchResults(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> {
            String message = messageService.getString("search.status.failed", task.getException().getMessage());
            statusMessage.set(message);
            appendLog("search.log.search.error", task.getException().getMessage());
            busy.set(false);
        }));
        executor.submit(task);
    }

    
    private void searchAll(String query) {
        busy.set(true);
        results.clear();
        statusMessage.set("");
        resetPackageState();
        clearOperationLog();
        appendLog("search.log.search.start", query, messageService.getString("common.source.all.short"));

        executor.submit(() -> {
            List<Package> merged = new ArrayList<>();
            for (PackageManagerService svc : List.of(wingetService, chocoService, scoopService)) {
                if (!svc.isAvailable()) continue;
                try {
                    Task<List<Package>> t = svc.search(query);
                    t.run();
                    merged.addAll(t.get());
                } catch (Exception e) {
                    log.warn("Search failed on {}: {}", svc.getDisplayName(), e.getMessage());
                }
            }
            final List<Package> finalResults = merged;
            finalizeSearchResults(finalResults);
        });
    }

    
    public void install(Package pkg,
                        Runnable onSuccess,
                        java.util.function.Consumer<String> onError) {
        if (pkg == null) return;
        busy.set(true);
        setInstalling(pkg, true);
        PackageManagerService service = resolveService(pkg.source());
        if (service == null || !service.isAvailable()) {
            busy.set(false);
            setInstalling(pkg, false);
            String message = messageService.getString("search.status.source.unavailable", pkg.source().name());
            statusMessage.set(message);
            appendLog("search.log.install.error", message);
            onError.accept(message);
            return;
        }
        appendLog("search.log.install.start", pkg.name(), pkg.source().name());
        Task<Void> task = service.install(pkg);
        task.setOnSucceeded(e -> executor.submit(() -> verifyInstallation(pkg, service, onSuccess, onError)));
        task.setOnFailed(e -> Platform.runLater(() -> {
            busy.set(false);
            setInstalling(pkg, false);
            String message = task.getException() != null ? task.getException().getMessage() : "Unknown install error";
            appendLog("search.log.install.error", message);
            onError.accept(message);
        }));
        executor.submit(task);
    }

    private void verifyInstallation(Package requestedPackage,
                                    PackageManagerService service,
                                    Runnable onSuccess,
                                    java.util.function.Consumer<String> onError) {
        try {
            Task<List<Package>> verifyTask = service.getInstalled();
            verifyTask.run();
            boolean installed = verifyTask.get().stream().anyMatch(installedPackage -> matchesPackage(installedPackage, requestedPackage));
            Platform.runLater(() -> {
                busy.set(false);
                setInstalling(requestedPackage, false);
                if (installed) {
                    addPackageKeys(installedPackageKeys, requestedPackage);
                    markStateChanged();
                    appendLog("search.log.install.success", requestedPackage.name());
                    onSuccess.run();
                    return;
                }
                String message = messageService.getString("search.install.verify.failed", requestedPackage.name());
                statusMessage.set(message);
                appendLogRaw(message);
                onError.accept(message);
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                busy.set(false);
                setInstalling(requestedPackage, false);
                String message = e.getMessage() != null ? e.getMessage() : "Unknown install verification error";
                appendLog("search.log.install.error", message);
                onError.accept(message);
            });
        }
    }

    private boolean matchesPackage(Package installedPackage, Package requestedPackage) {
        if (installedPackage == null || requestedPackage == null) return false;
        if (installedPackage.source() != requestedPackage.source()) return false;
        return equalsIgnoreCase(installedPackage.id(), requestedPackage.id())
                || equalsIgnoreCase(installedPackage.name(), requestedPackage.name());
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    
    public boolean isInstalled(Package pkg) {
        if (pkg == null || pkg.source() == null) {
            return false;
        }
        return installedPackageKeys.contains(buildPackageKey(pkg.source(), pkg.id()))
                || installedPackageKeys.contains(buildPackageKey(pkg.source(), pkg.name()));
    }

    
    public boolean isInstalling(Package pkg) {
        if (pkg == null || pkg.source() == null) {
            return false;
        }
        return installingPackageKeys.contains(buildPackageKey(pkg.source(), pkg.id()))
                || installingPackageKeys.contains(buildPackageKey(pkg.source(), pkg.name()));
    }

    private void clearOperationLog() {
        runOnFxThread(() -> operationLog.set(""));
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

    private void finalizeSearchResults(List<Package> searchResults) {
        List<Package> safeResults = searchResults == null ? List.of() : searchResults.stream()
                .filter(Objects::nonNull)
                .toList();
        Set<String> resolvedInstalledPackages = resolveInstalledPackageKeys(safeResults);
        Platform.runLater(() -> {
            installedPackageKeys.clear();
            installedPackageKeys.addAll(resolvedInstalledPackages);
            results.setAll(safeResults);
            if (results.isEmpty()) {
                statusMessage.set(messageService.getString("search.no.results"));
            }
            appendLog("search.log.search.success", results.size());
            busy.set(false);
            markStateChanged();
        });
    }

    private Set<String> resolveInstalledPackageKeys(List<Package> searchResults) {
        Set<String> keys = ConcurrentHashMap.newKeySet();
        Set<PackageSource> requestedSources = searchResults.stream()
                .map(Package::source)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        for (PackageSource source : requestedSources) {
            PackageManagerService service = resolveService(source);
            if (service == null || !service.isAvailable()) {
                continue;
            }
            try {
                Task<List<Package>> installedTask = service.getInstalled();
                installedTask.run();
                for (Package installedPackage : installedTask.get()) {
                    addPackageKeys(keys, installedPackage);
                }
            } catch (Exception ex) {
                log.warn("Failed to resolve installed packages for {}: {}", source, ex.getMessage());
            }
        }
        return keys;
    }

    private void addPackageKeys(Set<String> keys, Package pkg) {
        if (keys == null || pkg == null || pkg.source() == null) {
            return;
        }
        addPackageKey(keys, pkg.source(), pkg.id());
        addPackageKey(keys, pkg.source(), pkg.name());
    }

    private void addPackageKey(Set<String> keys, PackageSource source, String value) {
        String normalizedKey = buildPackageKey(source, value);
        if (normalizedKey != null) {
            keys.add(normalizedKey);
        }
    }

    private String buildPackageKey(PackageSource source, String value) {
        if (source == null || value == null || value.isBlank()) {
            return null;
        }
        return source.name() + "|" + value.trim().toLowerCase(Locale.ROOT);
    }

    private void setInstalling(Package pkg, boolean installing) {
        addOrRemovePackageKeys(installingPackageKeys, pkg, installing);
        markStateChanged();
    }

    private void resetPackageState() {
        installedPackageKeys.clear();
        installingPackageKeys.clear();
        markStateChanged();
    }

    private void addOrRemovePackageKeys(Set<String> keys, Package pkg, boolean add) {
        if (pkg == null || pkg.source() == null) {
            return;
        }
        updatePackageKey(keys, pkg.source(), pkg.id(), add);
        updatePackageKey(keys, pkg.source(), pkg.name(), add);
    }

    private void updatePackageKey(Set<String> keys, PackageSource source, String value, boolean add) {
        String normalizedKey = buildPackageKey(source, value);
        if (normalizedKey == null) {
            return;
        }
        if (add) {
            keys.add(normalizedKey);
            return;
        }
        keys.remove(normalizedKey);
    }

    private void markStateChanged() {
        runOnFxThread(() -> stateRevision.set(stateRevision.get() + 1));
    }

    public ObservableList<Package> getResults()    { return results; }
    public StringProperty  statusMessageProperty() { return statusMessage; }
    public BooleanProperty busyProperty()          { return busy; }
    public StringProperty  operationLogProperty()  { return operationLog; }
    public ReadOnlyIntegerProperty stateRevisionProperty() { return stateRevision.getReadOnlyProperty(); }
}

