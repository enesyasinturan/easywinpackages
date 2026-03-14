package com.softeyt.easywinpackages.view.settings;

import com.google.inject.Inject;
import com.softeyt.easywinpackages.i18n.MessageService;
import com.softeyt.easywinpackages.view.main.AlertHelper;
import com.softeyt.easywinpackages.viewmodel.SettingsViewModel;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.ResourceBundle;


public class SettingsController implements Initializable {

    @FXML private Label       lblTitle;
    @FXML private Label       lblThemeTitle;
    @FXML private Label       lblLangTitle;
    @FXML private Label       lblLogsTitle;
    @FXML private Label       lblLogFileTitle;
    @FXML private Label       lblLogFolderTitle;
    @FXML private ComboBox<String> cmbTheme;
    @FXML private ComboBox<String> cmbLanguage;
    @FXML private TextField   txtLogFile;
    @FXML private TextField   txtLogFolder;
    @FXML private Button      btnOpenLogFile;
    @FXML private Button      btnOpenLogFolder;
    @FXML private Label       lblSaved;

    @Inject private SettingsViewModel viewModel;
    @Inject private MessageService    messageService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
        lblTitle.textProperty().bind(messageService.bind("settings.title"));
        lblThemeTitle.textProperty().bind(messageService.bind("settings.theme"));
        lblLangTitle.textProperty().bind(messageService.bind("settings.language"));
        lblLogsTitle.textProperty().bind(messageService.bind("settings.logs"));
        lblLogFileTitle.textProperty().bind(messageService.bind("settings.log.file"));
        lblLogFolderTitle.textProperty().bind(messageService.bind("settings.log.folder"));
        btnOpenLogFile.textProperty().bind(messageService.bind("settings.log.file.open"));
        btnOpenLogFolder.textProperty().bind(messageService.bind("settings.log.folder.open"));

        txtLogFile.textProperty().bind(viewModel.currentLogFilePathProperty());
        txtLogFolder.textProperty().bind(viewModel.logsDirectoryPathProperty());
        txtLogFile.setEditable(false);
        txtLogFolder.setEditable(false);

        cmbTheme.getItems().setAll("SYSTEM", "LIGHT", "DARK");
        cmbTheme.setConverter(new StringConverter<>() {
            @Override public String toString(String theme) {
                if (theme == null) return "";
                return switch (theme) {
                    case "SYSTEM" -> messageService.getString("settings.theme.system");
                    case "DARK" -> messageService.getString("settings.theme.dark");
                    default -> messageService.getString("settings.theme.light");
                };
            }

            @Override public String fromString(String value) {
                return value;
            }
        });
        cmbTheme.getSelectionModel().select(viewModel.selectedThemeProperty().get());
        cmbTheme.setOnAction(e -> onThemeChanged());

        
        cmbLanguage.getItems().addAll("en", "tr");
        cmbLanguage.setConverter(new StringConverter<>() {
            @Override public String toString(String lang) {
                if (lang == null) return "";
                return "tr".equals(lang)
                        ? messageService.getString("settings.language.tr")
                        : messageService.getString("settings.language.en");
            }
            @Override public String fromString(String s) { return s; }
        });
        cmbLanguage.getSelectionModel().select(viewModel.selectedLocaleProperty().get());
        cmbLanguage.setOnAction(e -> {
            String sel = cmbLanguage.getValue();
            if (sel != null) { viewModel.applyLocale(sel); showSaved(); }
        });
    }

    @FXML
    private void onThemeChanged() {
        String theme = cmbTheme.getValue();
        if (theme != null) {
            viewModel.applyTheme(theme);
            showSaved();
        }
    }

    @FXML
    private void onOpenLogFile() {
        try {
            viewModel.openCurrentLogFile();
        } catch (Exception ex) {
            AlertHelper.showError(btnOpenLogFile.getScene(),
                    messageService.getString("common.error"), ex.getMessage());
        }
    }

    @FXML
    private void onOpenLogFolder() {
        try {
            viewModel.openLogsDirectory();
        } catch (Exception ex) {
            AlertHelper.showError(btnOpenLogFolder.getScene(),
                    messageService.getString("common.error"), ex.getMessage());
        }
    }

    private void showSaved() {
        lblSaved.textProperty().bind(messageService.bind("settings.saved"));
        lblSaved.setVisible(true);
        lblSaved.setManaged(true);
        new Timeline(new KeyFrame(Duration.seconds(2),
                e -> { lblSaved.setVisible(false); lblSaved.setManaged(false); })).play();
    }
}
