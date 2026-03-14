package com.softeyt.easywinpackages.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.softeyt.easywinpackages.model.AppSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


@Singleton
public class SettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);
    private static final Path SETTINGS_DIR =
            Path.of(System.getenv("APPDATA"), "EasyWinPackages");
    private static final Path SETTINGS_FILE = SETTINGS_DIR.resolve("settings.json");

    private final ObjectMapper mapper;
    private AppSettings current;

    @Inject
    public SettingsService(ObjectMapper mapper) {
        this.mapper = mapper;
        this.current = load();
    }

    
    public AppSettings getSettings() {
        return current;
    }

    
    public void save(AppSettings settings) {
        if (settings == null) throw new IllegalArgumentException("settings must not be null");
        try {
            Files.createDirectories(SETTINGS_DIR);
            mapper.writeValue(SETTINGS_FILE.toFile(), settings);
            current = settings;
            log.info("Settings saved to {}", SETTINGS_FILE);
        } catch (IOException e) {
            log.error("Failed to save settings: {}", e.getMessage(), e);
        }
    }

    private AppSettings load() {
        if (!Files.exists(SETTINGS_FILE)) {
            log.info("No settings file found at {}; using defaults", SETTINGS_FILE);
            return AppSettings.defaults();
        }
        try {
            AppSettings loaded = mapper.readValue(SETTINGS_FILE.toFile(), AppSettings.class);
            log.info("Settings loaded from {}", SETTINGS_FILE);
            return loaded;
        } catch (IOException e) {
            log.warn("Failed to read settings file; using defaults. Cause: {}", e.getMessage());
            return AppSettings.defaults();
        }
    }
}

