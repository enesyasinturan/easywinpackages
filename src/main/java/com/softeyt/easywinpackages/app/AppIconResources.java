package com.softeyt.easywinpackages.app;

import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;


public final class AppIconResources {

    private static final Logger log = LoggerFactory.getLogger(AppIconResources.class);

    
    public static final String PRIMARY_WINDOW_ICON = "/com/softeyt/easywinpackages/icons/easywinpkg.png";

    
    public static final List<String> WINDOW_ICON_RESOURCES = List.of(PRIMARY_WINDOW_ICON);

    private AppIconResources() {
    }

    
    public static void applyWindowIcons(Stage stage, Class<?> anchor) {
        if (stage == null || anchor == null) {
            throw new IllegalArgumentException("stage and anchor must not be null");
        }
        stage.getIcons().clear();
        for (String resourcePath : WINDOW_ICON_RESOURCES) {
            URL resource = anchor.getResource(resourcePath);
            if (resource == null) {
                log.warn("Icon resource not found: {}", resourcePath);
                continue;
            }
            stage.getIcons().add(new Image(resource.toExternalForm()));
        }
    }
}

