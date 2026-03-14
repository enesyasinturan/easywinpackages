package com.softeyt.easywinpackages.view.main;

import com.softeyt.easywinpackages.app.AppIconResources;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;

import java.util.Optional;


public final class AlertHelper {

    private AlertHelper() {}

    
    public static Optional<ButtonType> showInfo(Scene ownerScene, String title, String message) {
        Alert alert = build(Alert.AlertType.INFORMATION, ownerScene, title, message);
        return alert.showAndWait();
    }

    
    public static Optional<ButtonType> showError(Scene ownerScene, String title, String message) {
        Alert alert = build(Alert.AlertType.ERROR, ownerScene, title, message);
        return alert.showAndWait();
    }

    
    public static Optional<ButtonType> showWarning(Scene ownerScene, String title, String message) {
        Alert alert = build(Alert.AlertType.WARNING, ownerScene, title, message);
        return alert.showAndWait();
    }

    
    public static boolean showConfirmation(Scene ownerScene, String title, String message,
                                           String okLabel, String cancelLabel) {
        ButtonType okBtn     = new ButtonType(okLabel,     ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType(cancelLabel, ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = build(Alert.AlertType.CONFIRMATION, ownerScene, title, message);
        alert.getButtonTypes().setAll(okBtn, cancelBtn);
        return alert.showAndWait().map(b -> b == okBtn).orElse(false);
    }

    
    public static Optional<ButtonType> showCustom(Scene ownerScene, Alert.AlertType type,
                                                   String title, String message,
                                                   ButtonType... buttons) {
        Alert alert = build(type, ownerScene, title, message);
        alert.getButtonTypes().setAll(buttons);
        return alert.showAndWait();
    }

    

    private static Alert build(Alert.AlertType type, Scene ownerScene, String title, String message) {
        Alert alert = new Alert(type);
        if (ownerScene != null && ownerScene.getWindow() != null) {
            alert.initOwner(ownerScene.getWindow());
        }
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyTheme(alert, ownerScene);
        return alert;
    }

    private static void applyTheme(Alert alert, Scene ownerScene) {
        DialogPane pane = alert.getDialogPane();
        if (ownerScene != null && !ownerScene.getStylesheets().isEmpty()) {
            pane.getStylesheets().addAll(ownerScene.getStylesheets());
        }
        pane.getStyleClass().add("app-dialog");
        pane.sceneProperty().addListener((obs, oldScene, newScene) -> applyWindowIcons(newScene));
        applyWindowIcons(pane.getScene());
    }

    private static void applyWindowIcons(Scene scene) {
        if (scene == null || !(scene.getWindow() instanceof Stage stage) || !stage.getIcons().isEmpty()) {
            return;
        }
        AppIconResources.applyWindowIcons(stage, AlertHelper.class);
    }
}

