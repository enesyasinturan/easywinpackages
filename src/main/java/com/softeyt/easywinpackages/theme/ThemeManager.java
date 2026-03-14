package com.softeyt.easywinpackages.theme;

import com.google.inject.Singleton;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


@Singleton
public class ThemeManager {

    private static final Logger log = LoggerFactory.getLogger(ThemeManager.class);

    private static final String LIGHT_CSS = "/com/softeyt/easywinpackages/themes/light-theme.css";
    private static final String DARK_CSS  = "/com/softeyt/easywinpackages/themes/dark-theme.css";
    private static final String PERSONALIZE_KEY =
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";

    private Scene activeScene;
    private Theme currentTheme = Theme.SYSTEM;
    private Theme effectiveTheme = Theme.LIGHT;

    
    public void apply(Scene scene, Theme theme) {
        this.activeScene = Objects.requireNonNull(scene, "scene must not be null");
        applyTheme(theme);
    }

    
    public void switchTheme(Theme theme) {
        if (activeScene == null) {
            log.warn("switchTheme called before a scene was registered");
            return;
        }
        applyTheme(theme);
    }

    
    public Theme getCurrentTheme() {
        return currentTheme;
    }

    
    public Theme getEffectiveTheme() {
        return effectiveTheme;
    }

    private void applyTheme(Theme theme) {
        Theme resolvedTheme = resolveTheme(theme);
        String cssPath = resolvedTheme == Theme.DARK ? DARK_CSS : LIGHT_CSS;
        var stylesheets = activeScene.getStylesheets();
        stylesheets.clear();
        var url = getClass().getResource(cssPath);
        if (url == null) {
            log.error("CSS resource not found: {}", cssPath);
            return;
        }
        stylesheets.add(url.toExternalForm());
        currentTheme = theme;
        effectiveTheme = resolvedTheme;
        log.debug("Applied theme: {} (effective={})", theme, resolvedTheme);
    }

    
    public Theme resolveTheme(Theme theme) {
        if (theme != Theme.SYSTEM) {
            return theme;
        }
        return detectSystemTheme();
    }

    
    public Theme detectSystemTheme() {
        try {
            Process process = new ProcessBuilder(
                    "reg", "query", PERSONALIZE_KEY, "/v", "AppsUseLightTheme")
                    .redirectErrorStream(true)
                    .start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().reduce("", (left, right) -> left + "\n" + right).trim();
            }
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished || process.exitValue() != 0) {
                log.debug("System theme registry query did not complete successfully.");
                return Theme.LIGHT;
            }
            return output.contains("0x0") ? Theme.DARK : Theme.LIGHT;
        } catch (Exception ex) {
            log.debug("Failed to detect Windows system theme: {}", ex.getMessage());
            return Theme.LIGHT;
        }
    }
}

