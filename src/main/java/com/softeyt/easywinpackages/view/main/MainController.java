package com.softeyt.easywinpackages.view.main;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.softeyt.easywinpackages.app.AppGlyphIcons;
import com.softeyt.easywinpackages.i18n.MessageService;
import com.softeyt.easywinpackages.theme.ThemeManager;
import com.softeyt.easywinpackages.view.dashboard.DashboardController;
import com.softeyt.easywinpackages.view.sources.SourcesController;
import com.softeyt.easywinpackages.view.uninstall.UninstallController;
import com.softeyt.easywinpackages.view.updates.UpdatesController;
import com.softeyt.easywinpackages.viewmodel.MainViewModel;
import com.softeyt.easywinpackages.viewmodel.SettingsViewModel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.Ikon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;


public class MainController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @FXML private HBox      rootHBox;
    @FXML private StackPane contentArea;
    @FXML private Button    btnDashboard;
    @FXML private Button    btnInstall;
    @FXML private Button    btnUpdates;
    @FXML private Button    btnUninstall;
    @FXML private Button    btnBundle;
    @FXML private Button    btnSources;
    @FXML private Button    btnSettings;

    @Inject private MainViewModel     mainViewModel;
    @Inject private SettingsViewModel settingsViewModel;
    @Inject private MessageService    messageService;
    @Inject private ThemeManager      themeManager;
    @Inject private Injector          injector;

    private final Map<String, Node> screenCache = new HashMap<>();
    private Button currentActiveButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureNavButtons();
        bindNavLabels();
        navigateTo("DASHBOARD", btnDashboard);
    }

    

    @FXML private void onDashboard()  { navigateTo("DASHBOARD",  btnDashboard); }
    @FXML private void onInstall()     { navigateTo("INSTALL",    btnInstall); }
    @FXML private void onUpdates()    { navigateTo("UPDATES",    btnUpdates); }
    @FXML private void onUninstall()  { navigateTo("UNINSTALL",  btnUninstall); }
    @FXML private void onBundle()     { navigateTo("BUNDLE",     btnBundle); }
    @FXML private void onSources()    { navigateTo("SOURCES",    btnSources); }
    @FXML private void onSettings()   { navigateTo("SETTINGS",   btnSettings); }


    

    
    public void navigateToScreen(String screen) {
        if (contentArea == null) {
            
            javafx.application.Platform.runLater(() -> navigateToScreen(screen));
            return;
        }
        Button btn = resolveNavButton(screen);
        navigateTo(screen, btn != null ? btn : btnDashboard);
    }

    private void navigateTo(String screen, Button navButton) {
        if (currentActiveButton != null) currentActiveButton.getStyleClass().remove("active");
        navButton.getStyleClass().add("active");
        currentActiveButton = navButton;
        mainViewModel.navigateTo(screen);

        Node content = screenCache.computeIfAbsent(screen, this::loadScreen);
        contentArea.getChildren().setAll(content);

        
        javafx.application.Platform.runLater(() -> notifyScreenActivated(screen));
    }

    private Button resolveNavButton(String screen) {
        return switch (screen) {
            case "DASHBOARD" -> btnDashboard;
            case "INSTALL"    -> btnInstall;
            case "UPDATES"   -> btnUpdates;
            case "UNINSTALL" -> btnUninstall;
            case "BUNDLE"    -> btnBundle;
            case "SOURCES"   -> btnSources;
            case "SETTINGS"  -> btnSettings;
            default          -> btnDashboard;
        };
    }

    private Node loadScreen(String screen) {
        String fxmlPath = resolveFxmlPath(screen);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(injector::getInstance);
            return loader.load();
        } catch (IOException e) {
            log.error("Failed to load screen '{}' from {}: {}", screen, fxmlPath, e.getMessage(), e);
            return new javafx.scene.control.Label("Error loading screen: " + screen);
        }
    }

    private void notifyScreenActivated(String screen) {
        switch (screen) {
            case "DASHBOARD" -> injector.getInstance(
                    DashboardController.class).onNavigatedTo();
            case "UPDATES"   -> injector.getInstance(
                    UpdatesController.class).onNavigatedTo();
            case "UNINSTALL" -> injector.getInstance(
                    UninstallController.class).onNavigatedTo();
            case "SOURCES"   -> injector.getInstance(
                    SourcesController.class).onNavigatedTo();
            default -> {}
        }
    }

    private String resolveFxmlPath(String screen) {
        return switch (screen) {
            case "DASHBOARD" -> "/com/softeyt/easywinpackages/view/dashboard/dashboard-view.fxml";
            case "INSTALL"    -> "/com/softeyt/easywinpackages/view/install/install-view.fxml";
            case "UPDATES"   -> "/com/softeyt/easywinpackages/view/updates/updates-view.fxml";
            case "UNINSTALL" -> "/com/softeyt/easywinpackages/view/uninstall/uninstall-view.fxml";
            case "BUNDLE"    -> "/com/softeyt/easywinpackages/view/bundle/bundle-view.fxml";
            case "SOURCES"   -> "/com/softeyt/easywinpackages/view/sources/sources-view.fxml";
            case "SETTINGS"  -> "/com/softeyt/easywinpackages/view/settings/settings-view.fxml";
            default -> throw new IllegalArgumentException("Unknown screen: " + screen);
        };
    }

    private void bindNavLabels() {
        btnDashboard.textProperty().bind(messageService.bind("nav.dashboard"));
        btnInstall.textProperty().bind(messageService.bind("nav.install"));
        btnUpdates.textProperty().bind(messageService.bind("nav.updates"));
        btnUninstall.textProperty().bind(messageService.bind("nav.uninstall"));
        btnBundle.textProperty().bind(messageService.bind("nav.bundle"));
        btnSources.textProperty().bind(messageService.bind("nav.sources"));
        btnSettings.textProperty().bind(messageService.bind("nav.settings"));
    }

    private void configureNavButtons() {
        configureNavButton(btnDashboard, AppGlyphIcons.NAV_DASHBOARD);
        configureNavButton(btnInstall, AppGlyphIcons.NAV_INSTALL);
        configureNavButton(btnUpdates, AppGlyphIcons.NAV_UPDATES);
        configureNavButton(btnUninstall, AppGlyphIcons.NAV_UNINSTALL);
        configureNavButton(btnBundle, AppGlyphIcons.NAV_BUNDLE);
        configureNavButton(btnSources, AppGlyphIcons.NAV_SOURCES);
        configureNavButton(btnSettings, AppGlyphIcons.NAV_SETTINGS);
    }

    private void configureNavButton(Button button, Ikon iconCode) {
        Node icon = AppGlyphIcons.createNavigationIcon(iconCode);
        button.setGraphic(icon);
        button.setGraphicTextGap(12);
        button.setContentDisplay(ContentDisplay.LEFT);
    }
}


