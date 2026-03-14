package com.softeyt.easywinpackages.viewmodel;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.softeyt.easywinpackages.model.UpdateInfo;
import com.softeyt.easywinpackages.service.impl.ChocoService;
import com.softeyt.easywinpackages.service.impl.ScoopService;
import com.softeyt.easywinpackages.service.impl.WingetService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Singleton
public class DashboardViewModel {

    private final WingetService wingetService;
    private final ChocoService  chocoService;
    private final ScoopService  scoopService;

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "dashboard-loader");
        t.setDaemon(true);
        return t;
    });

    private final IntegerProperty updatesCount   = new SimpleIntegerProperty(-1);
    private final IntegerProperty totalInstalled = new SimpleIntegerProperty(-1);

    private final BooleanProperty updatesLoading = new SimpleBooleanProperty(false);
    private final BooleanProperty totalLoading   = new SimpleBooleanProperty(false);

    @Inject
    public DashboardViewModel(WingetService wingetService,
                               ChocoService chocoService,
                               ScoopService scoopService) {
        this.wingetService = wingetService;
        this.chocoService  = chocoService;
        this.scoopService  = scoopService;
    }

    
    public void refresh() {
        loadUpdates();
        loadTotalInstalled();
    }

    

    private void loadUpdates() {
        updatesLoading.set(true);
        executor.submit(() -> {
            int total = 0;
            if (wingetService.isAvailable()) {
                try {
                    Task<List<UpdateInfo>> t = wingetService.getUpdates();
                    t.run();
                    total += t.get().size();
                } catch (Exception ignored) {}
            }
            if (chocoService.isAvailable()) {
                try {
                    Task<List<UpdateInfo>> t = chocoService.getUpdates();
                    t.run();
                    total += t.get().size();
                } catch (Exception ignored) {}
            }
            if (scoopService.isAvailable()) {
                try {
                    Task<List<UpdateInfo>> t = scoopService.getUpdates();
                    t.run();
                    total += t.get().size();
                } catch (Exception ignored) {}
            }
            final int finalTotal = total;
            Platform.runLater(() -> {
                updatesCount.set(finalTotal);
                updatesLoading.set(false);
            });
        });
    }

    private void loadTotalInstalled() {
        totalLoading.set(true);
        executor.submit(() -> {
            int total = 0;
            if (wingetService.isAvailable()) {
                try {
                    Task<Integer> t = wingetService.getInstalledCount();
                    t.run();
                    total += t.get();
                } catch (Exception ignored) {}
            }
            if (chocoService.isAvailable()) {
                try {
                    Task<Integer> t = chocoService.getInstalledCount();
                    t.run();
                    total += t.get();
                } catch (Exception ignored) {}
            }
            if (scoopService.isAvailable()) {
                try {
                    Task<Integer> t = scoopService.getInstalledCount();
                    t.run();
                    total += t.get();
                } catch (Exception ignored) {}
            }
            final int finalTotal = total;
            Platform.runLater(() -> {
                totalInstalled.set(finalTotal);
                totalLoading.set(false);
            });
        });
    }

    

    public IntegerProperty updatesCountProperty()   { return updatesCount; }
    public IntegerProperty totalInstalledProperty() { return totalInstalled; }
    public BooleanProperty updatesLoadingProperty() { return updatesLoading; }
    public BooleanProperty totalLoadingProperty()   { return totalLoading; }
}

