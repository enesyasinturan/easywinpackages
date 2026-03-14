package com.softeyt.easywinpackages.view.sources;

import com.google.inject.Inject;
import com.softeyt.easywinpackages.i18n.MessageService;
import com.softeyt.easywinpackages.view.main.AlertHelper;
import com.softeyt.easywinpackages.view.main.Refreshable;
import com.softeyt.easywinpackages.viewmodel.SourcesViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;


public class SourcesController implements Initializable, Refreshable {

    @FXML private Label             lblTitle;
    @FXML private Label             lblSubtitle;
    @FXML private Button            btnRefresh;
    @FXML private Label             lblWingetName;
    @FXML private Label             lblChocoName;
    @FXML private Label             lblScoopName;
    @FXML private Label             lblWingetDesc;
    @FXML private Label             lblChocoDesc;
    @FXML private Label             lblScoopDesc;
    @FXML private ProgressIndicator spinWinget;
    @FXML private ProgressIndicator spinChoco;
    @FXML private ProgressIndicator spinScoop;
    @FXML private Label             lblWingetStatus;
    @FXML private Label             lblChocoStatus;
    @FXML private Label             lblScoopStatus;
    @FXML private Button            btnInstallWinget;
    @FXML private Button            btnInstallChoco;
    @FXML private Button            btnInstallScoop;

    @Inject private SourcesViewModel viewModel;
    @Inject private MessageService   messageService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblTitle.textProperty().bind(messageService.bind("sources.title"));
        lblSubtitle.textProperty().bind(messageService.bind("sources.subtitle"));
        btnRefresh.textProperty().bind(messageService.bind("dashboard.refresh"));

        lblWingetName.textProperty().bind(messageService.bind("sources.winget"));
        lblChocoName.textProperty().bind(messageService.bind("sources.choco"));
        lblScoopName.textProperty().bind(messageService.bind("sources.scoop"));
        lblWingetDesc.textProperty().bind(messageService.bind("sources.winget.desc"));
        lblChocoDesc.textProperty().bind(messageService.bind("sources.choco.desc"));
        lblScoopDesc.textProperty().bind(messageService.bind("sources.scoop.desc"));

        btnInstallWinget.textProperty().bind(messageService.bind("sources.install"));
        btnInstallChoco.textProperty().bind(messageService.bind("sources.install"));
        btnInstallScoop.textProperty().bind(messageService.bind("sources.install"));

        bindSourceCard(viewModel.wingetAvailableProperty(), viewModel.wingetCheckingProperty(), viewModel.wingetOperationProperty(),
                spinWinget, lblWingetStatus, btnInstallWinget);
        bindSourceCard(viewModel.chocoAvailableProperty(), viewModel.chocoCheckingProperty(), viewModel.chocoOperationProperty(),
                spinChoco, lblChocoStatus, btnInstallChoco);
        bindSourceCard(viewModel.scoopAvailableProperty(), viewModel.scoopCheckingProperty(), viewModel.scoopOperationProperty(),
                spinScoop, lblScoopStatus, btnInstallScoop);
    }

    @Override public void onNavigatedTo() { viewModel.checkAll(); }

    @FXML private void onRefresh()       { viewModel.checkAll(); }

    @FXML private void onInstallWinget() {
        if (viewModel.wingetAvailableProperty().get()) {
            viewModel.removeWinget(
                    () -> AlertHelper.showInfo(btnInstallWinget.getScene(),
                            messageService.getString("common.success"),
                            messageService.getString("sources.remove.success", "Winget")),
                    err -> AlertHelper.showError(btnInstallWinget.getScene(),
                            messageService.getString("common.error"), err));
            return;
        }
        viewModel.installWinget(
                () -> AlertHelper.showInfo(btnInstallWinget.getScene(),
                        messageService.getString("common.success"),
                        messageService.getString("sources.install.success", "Winget")),
                err -> AlertHelper.showError(btnInstallWinget.getScene(),
                        messageService.getString("common.error"), err));
    }

    @FXML private void onInstallChoco() {
        if (viewModel.chocoAvailableProperty().get()) {
            viewModel.removeChoco(
                    () -> AlertHelper.showInfo(btnInstallChoco.getScene(),
                            messageService.getString("common.success"),
                            messageService.getString("sources.remove.success", "Chocolatey")),
                    err -> AlertHelper.showError(btnInstallChoco.getScene(),
                            messageService.getString("common.error"), err));
            return;
        }
        viewModel.installChoco(
                () -> AlertHelper.showInfo(btnInstallChoco.getScene(),
                        messageService.getString("common.success"),
                        messageService.getString("sources.install.success", "Chocolatey")),
                err -> AlertHelper.showError(btnInstallChoco.getScene(),
                        messageService.getString("common.error"), err));
    }

    @FXML private void onInstallScoop() {
        if (viewModel.scoopAvailableProperty().get()) {
            viewModel.removeScoop(
                    () -> AlertHelper.showInfo(btnInstallScoop.getScene(),
                            messageService.getString("common.success"),
                            messageService.getString("sources.remove.success", "Scoop")),
                    err -> AlertHelper.showError(btnInstallScoop.getScene(),
                            messageService.getString("common.error"), err));
            return;
        }
        viewModel.installScoop(
                () -> AlertHelper.showInfo(btnInstallScoop.getScene(),
                        messageService.getString("common.success"),
                        messageService.getString("sources.install.success", "Scoop")),
                err -> AlertHelper.showError(btnInstallScoop.getScene(),
                        messageService.getString("common.error"), err));
    }

    

    
    private void bindSourceCard(BooleanProperty available,
                                BooleanProperty checking,
                                javafx.beans.property.ReadOnlyObjectProperty<SourcesViewModel.SourceOperation> operation,
                                ProgressIndicator spinner, Label statusLabel, Button installBtn) {
        spinner.visibleProperty().bind(checking);
        spinner.managedProperty().bind(checking);
        installBtn.setVisible(true);
        installBtn.setManaged(true);
        installBtn.disableProperty().bind(checking);

        statusLabel.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> resolveStatusText(available.get(), operation.get()),
                available, operation, messageService.localeProperty()));
        installBtn.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> resolveButtonText(available.get(), operation.get()),
                available, operation, messageService.localeProperty()));

        javafx.beans.value.ChangeListener<Object> stateListener = (obs, oldValue, newValue) ->
                applyStatusStyle(available.get(), operation.get(), statusLabel, installBtn);
        available.addListener(stateListener);
        operation.addListener(stateListener);
        applyStatusStyle(available.get(), operation.get(), statusLabel, installBtn);
    }

    private void applyStatusStyle(boolean isAvailable,
                                  SourcesViewModel.SourceOperation operation,
                                  Label statusLabel,
                                  Button installBtn) {
        boolean inProgress = operation == SourcesViewModel.SourceOperation.CHECKING
                || operation == SourcesViewModel.SourceOperation.INSTALLING
                || operation == SourcesViewModel.SourceOperation.REMOVING;
        statusLabel.getStyleClass().setAll(inProgress ? "badge-info" : isAvailable ? "badge-success" : "badge-danger");
        installBtn.getStyleClass().setAll("button",
                operation == SourcesViewModel.SourceOperation.REMOVING || (operation == SourcesViewModel.SourceOperation.IDLE && isAvailable)
                        ? "btn-danger"
                        : operation == SourcesViewModel.SourceOperation.INSTALLING
                        ? "btn-secondary"
                        : "btn-primary");
    }

    private String resolveStatusText(boolean isAvailable, SourcesViewModel.SourceOperation operation) {
        return switch (operation) {
            case INSTALLING -> messageService.getString("sources.installing");
            case REMOVING -> messageService.getString("sources.removing");
            case CHECKING -> messageService.getString("sources.checking");
            case IDLE -> isAvailable
                    ? messageService.getString("sources.status.installed")
                    : messageService.getString("sources.status.not.installed");
        };
    }

    private String resolveButtonText(boolean isAvailable, SourcesViewModel.SourceOperation operation) {
        return switch (operation) {
            case INSTALLING -> messageService.getString("sources.installing");
            case REMOVING -> messageService.getString("sources.removing");
            case CHECKING -> messageService.getString("sources.checking");
            case IDLE -> isAvailable
                    ? messageService.getString("sources.remove")
                    : messageService.getString("sources.install");
        };
    }
}
