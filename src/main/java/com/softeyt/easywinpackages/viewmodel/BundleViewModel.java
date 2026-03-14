package com.softeyt.easywinpackages.viewmodel;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.softeyt.easywinpackages.model.BundleEntry;
import com.softeyt.easywinpackages.model.BundleManifest;
import com.softeyt.easywinpackages.model.Package;
import com.softeyt.easywinpackages.service.BundleService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


@Singleton
public class BundleViewModel {

    private final BundleService        bundleService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "bundle-worker");
        t.setDaemon(true);
        return t;
    });

    private final ObservableList<BundleEntry> entries = FXCollections.observableArrayList();
    private final StringProperty  bundleName    = new SimpleStringProperty("");
    private final StringProperty  statusMessage = new SimpleStringProperty("");
    private final BooleanProperty busy          = new SimpleBooleanProperty(false);
    private final DoubleProperty  progress      = new SimpleDoubleProperty(0);

    @Inject
    public BundleViewModel(BundleService bundleService) {
        this.bundleService = bundleService;
    }

    
    public void addEntry(BundleEntry entry) {
        if (entry == null || containsEntry(entry)) {
            return;
        }
        entries.add(entry);
    }

    
    public boolean containsEntry(BundleEntry entry) {
        if (entry == null || entry.source() == null) {
            return false;
        }
        return entries.stream().anyMatch(existing -> matchesEntry(existing, entry));
    }

    
    public boolean containsEntryForPackage(Package pkg) {
        if (pkg == null || pkg.source() == null) {
            return false;
        }
        return entries.stream().anyMatch(existing -> matchesPackage(existing, pkg));
    }

    
    public void removeEntry(BundleEntry entry) {
        entries.remove(entry);
    }

    
    public void export(Path outputPath, Runnable onSuccess, Consumer<String> onError) {
        String name = bundleName.get();
        if (name == null || name.isBlank()) name = "My Bundle";
        final String finalName = name;
        executor.submit(() -> {
            try {
                bundleService.export(List.copyOf(entries), finalName, outputPath);
                Platform.runLater(() -> { statusMessage.set("Bundle exported successfully."); onSuccess.run(); });
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        });
    }

    
    public void importBundle(Path bundlePath, Consumer<List<String>> onMissingSources,
                             Runnable onSuccess, Consumer<String> onError) {
        executor.submit(() -> {
            try {
                BundleManifest manifest = bundleService.importBundle(bundlePath);
                List<String> missing = bundleService.validateSources(manifest);
                if (!missing.isEmpty()) {
                    Platform.runLater(() -> onMissingSources.accept(missing));
                    return;
                }
                Platform.runLater(() -> {
                    entries.setAll(manifest.packages());
                    bundleName.set(manifest.name());
                    onSuccess.run();
                });
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        });
    }

    
    public void installAll(Runnable onSuccess, Consumer<String> onError) {
        if (entries.isEmpty()) return;
        busy.set(true);
        progress.set(0);

        BundleManifest manifest = new BundleManifest(
                BundleManifest.CURRENT_VERSION, bundleName.get(),
                java.time.LocalDateTime.now(), List.copyOf(entries));

        Task<Void> task = bundleService.installAll(manifest);
        task.progressProperty().addListener((obs, o, n) -> Platform.runLater(() -> progress.set(n.doubleValue())));
        task.messageProperty().addListener((obs, o, n) -> Platform.runLater(() -> statusMessage.set(n)));
        task.setOnSucceeded(e -> Platform.runLater(() -> { busy.set(false); onSuccess.run(); }));
        task.setOnFailed(e -> Platform.runLater(() -> { busy.set(false); onError.accept(task.getException().getMessage()); }));
        executor.submit(task);
    }

    public ObservableList<BundleEntry> getEntries() { return entries; }
    public StringProperty  bundleNameProperty()     { return bundleName; }
    public StringProperty  statusMessageProperty()  { return statusMessage; }
    public BooleanProperty busyProperty()           { return busy; }
    public DoubleProperty  progressProperty()       { return progress; }

    private boolean matchesEntry(BundleEntry left, BundleEntry right) {
        if (left == null || right == null || left.source() != right.source()) {
            return false;
        }
        return equalsIgnoreCase(left.id(), right.id())
                || equalsIgnoreCase(left.name(), right.name());
    }

    private boolean matchesPackage(BundleEntry entry, Package pkg) {
        if (entry == null || pkg == null || entry.source() != pkg.source()) {
            return false;
        }
        return equalsIgnoreCase(entry.id(), pkg.id())
                || equalsIgnoreCase(entry.name(), pkg.name());
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }
}

