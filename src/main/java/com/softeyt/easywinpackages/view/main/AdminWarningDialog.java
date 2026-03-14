package com.softeyt.easywinpackages.view.main;

import com.softeyt.easywinpackages.i18n.MessageService;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;


public class AdminWarningDialog {

    private static final Logger log = LoggerFactory.getLogger(AdminWarningDialog.class);

    private AdminWarningDialog() {}

    
    public static void show(MessageService messageService) {
        ButtonType runAsAdmin = new ButtonType(
                messageService.getString("admin.warning.run"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(
                messageService.getString("admin.warning.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

        
        javafx.scene.Scene ownerScene = findActiveScene();
        Optional<ButtonType> result = AlertHelper.showCustom(
                ownerScene,
                Alert.AlertType.WARNING,
                messageService.getString("admin.warning.title"),
                messageService.getString("admin.warning.message").replace("\\n", "\n"),
                runAsAdmin, cancel);
    }

    
    private static javafx.scene.Scene findActiveScene() {
        for (javafx.stage.Stage stage : javafx.stage.Stage.getWindows()
                .stream()
                .filter(w -> w instanceof javafx.stage.Stage)
                .map(w -> (javafx.stage.Stage) w)
                .toList()) {
            if (stage.isShowing() && stage.getScene() != null) return stage.getScene();
        }
        return null;
    }
}
