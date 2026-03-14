package com.softeyt.easywinpackages.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.softeyt.easywinpackages.i18n.MessageService;
import com.softeyt.easywinpackages.service.BundleService;
import com.softeyt.easywinpackages.service.CommandExecutor;
import com.softeyt.easywinpackages.service.LogFileService;
import com.softeyt.easywinpackages.service.SettingsService;
import com.softeyt.easywinpackages.service.impl.ChocoService;
import com.softeyt.easywinpackages.service.impl.ScoopService;
import com.softeyt.easywinpackages.service.impl.WingetService;
import com.softeyt.easywinpackages.theme.ThemeManager;
import com.softeyt.easywinpackages.view.bundle.BundleController;
import com.softeyt.easywinpackages.view.dashboard.DashboardController;
import com.softeyt.easywinpackages.view.main.MainController;
import com.softeyt.easywinpackages.view.search.SearchController;
import com.softeyt.easywinpackages.view.settings.SettingsController;
import com.softeyt.easywinpackages.view.sources.SourcesController;
import com.softeyt.easywinpackages.view.uninstall.UninstallController;
import com.softeyt.easywinpackages.view.updates.UpdatesController;
import com.softeyt.easywinpackages.viewmodel.*;


public class AppModule extends AbstractModule {

    @Override
    protected void configure() {
        
        bind(CommandExecutor.class).in(Singleton.class);
        bind(WingetService.class).in(Singleton.class);
        bind(ChocoService.class).in(Singleton.class);
        bind(ScoopService.class).in(Singleton.class);
        bind(SettingsService.class).in(Singleton.class);
        bind(BundleService.class).in(Singleton.class);
        bind(LogFileService.class).in(Singleton.class);
        bind(MessageService.class).in(Singleton.class);
        bind(ThemeManager.class).in(Singleton.class);

        
        bind(MainViewModel.class).in(Singleton.class);
        bind(DashboardViewModel.class).in(Singleton.class);
        bind(SearchViewModel.class).in(Singleton.class);
        bind(UpdatesViewModel.class).in(Singleton.class);
        bind(UninstallViewModel.class).in(Singleton.class);
        bind(BundleViewModel.class).in(Singleton.class);
        bind(SourcesViewModel.class).in(Singleton.class);
        bind(SettingsViewModel.class).in(Singleton.class);

        
        bind(MainController.class).in(Singleton.class);
        bind(DashboardController.class).in(Singleton.class);
        bind(SearchController.class).in(Singleton.class);
        bind(UpdatesController.class).in(Singleton.class);
        bind(UninstallController.class).in(Singleton.class);
        bind(BundleController.class).in(Singleton.class);
        bind(SourcesController.class).in(Singleton.class);
        bind(SettingsController.class).in(Singleton.class);
    }

    
    @Provides
    @Singleton
    ObjectMapper provideObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }
}
