package com.softeyt.easywinpackages.viewmodel;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.softeyt.easywinpackages.i18n.MessageService;
import com.softeyt.easywinpackages.model.AppSettings;
import com.softeyt.easywinpackages.service.LogFileService;
import com.softeyt.easywinpackages.service.SettingsService;
import com.softeyt.easywinpackages.theme.Theme;
import com.softeyt.easywinpackages.theme.ThemeManager;
import javafx.beans.property.*;

import java.io.IOException;
import java.util.Locale;


@Singleton
public class SettingsViewModel {

    private final SettingsService settingsService;
    private final ThemeManager    themeManager;
    private final MessageService  messageService;
    private final LogFileService  logFileService;

    private final BooleanProperty adminWarning   = new SimpleBooleanProperty(true);
    private final StringProperty  selectedTheme  = new SimpleStringProperty("SYSTEM");
    private final StringProperty  selectedLocale = new SimpleStringProperty("en");
    private final StringProperty  defaultSource  = new SimpleStringProperty("WINGET");
    private final ReadOnlyStringWrapper currentLogFilePath = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper logsDirectoryPath = new ReadOnlyStringWrapper();

    @Inject
    public SettingsViewModel(SettingsService settingsService,
                              ThemeManager themeManager,
                              MessageService messageService,
                              LogFileService logFileService) {
        this.settingsService = settingsService;
        this.themeManager    = themeManager;
        this.messageService  = messageService;
        this.logFileService  = logFileService;
        refreshLogPaths();
        loadFromSettings();
    }

    
    public void loadFromSettings() {
        AppSettings s = settingsService.getSettings();
        selectedTheme.set(s.theme());
        selectedLocale.set(s.locale());
    }

    
    public void applyTheme(String themeStr) {
        selectedTheme.set(themeStr);
        Theme theme = Theme.fromStoredValue(themeStr);
        themeManager.switchTheme(theme);
        saveSettings();
    }

    
    public void applyLocale(String localeTag) {
        selectedLocale.set(localeTag);
        messageService.changeLocale(Locale.forLanguageTag(localeTag));
        saveSettings();
    }

    
    public void saveSettings() {
        AppSettings settings = new AppSettings(
                selectedTheme.get(),
                selectedLocale.get());
        settingsService.save(settings);
    }

    
    public void openCurrentLogFile() throws IOException {
        logFileService.openCurrentLogFile();
        refreshLogPaths();
    }

    
    public void openLogsDirectory() throws IOException {
        logFileService.openLogsDirectory();
        refreshLogPaths();
    }

    
    public void refreshLogPaths() {
        currentLogFilePath.set(logFileService.getCurrentLogFile().toString());
        logsDirectoryPath.set(logFileService.getLogsDirectory().toString());
    }

    public BooleanProperty adminWarningProperty()   { return adminWarning; }
    public StringProperty  selectedThemeProperty()  { return selectedTheme; }
    public StringProperty  selectedLocaleProperty() { return selectedLocale; }
    public StringProperty  defaultSourceProperty()  { return defaultSource; }
    public ReadOnlyStringProperty currentLogFilePathProperty() { return currentLogFilePath.getReadOnlyProperty(); }
    public ReadOnlyStringProperty logsDirectoryPathProperty()  { return logsDirectoryPath.getReadOnlyProperty(); }
}

