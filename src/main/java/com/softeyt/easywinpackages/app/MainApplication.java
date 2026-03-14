package com.softeyt.easywinpackages.app;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.softeyt.easywinpackages.i18n.MessageService;
import com.softeyt.easywinpackages.model.AppSettings;
import com.softeyt.easywinpackages.service.SettingsService;
import com.softeyt.easywinpackages.theme.Theme;
import com.softeyt.easywinpackages.theme.ThemeManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;


public class MainApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(MainApplication.class);

    
    static Injector injector;

    @Override
    public void start(Stage stage) throws IOException {
        AppRuntimePaths.initialize(MainApplication.class);
        injector = Guice.createInjector(new AppModule());

        
        SettingsService settingsService = injector.getInstance(SettingsService.class);
        MessageService  messageService  = injector.getInstance(MessageService.class);
        ThemeManager    themeManager    = injector.getInstance(ThemeManager.class);

        AppSettings settings = settingsService.getSettings();
        messageService.changeLocale(Locale.forLanguageTag(settings.locale()));

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/softeyt/easywinpackages/view/main/main-view.fxml"));
        loader.setControllerFactory(injector::getInstance);

        Scene scene = new Scene(loader.load(), 1360, 820);

        Theme theme = Theme.fromStoredValue(settings.theme());
        themeManager.apply(scene, theme);

        stage.setTitle(messageService.getString("app.title"));
        AppIconResources.applyWindowIcons(stage, MainApplication.class);
        stage.setMinWidth(1080);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();

        log.info("EasyWinPackages started. Theme={}, Locale={}, LogFile={}",
                settings.theme(), settings.locale(), AppRuntimePaths.getCurrentLogFile());
    }

    public static void main(String[] args) {
        AppRuntimePaths.initialize(MainApplication.class);
        launch(args);
    }
}

