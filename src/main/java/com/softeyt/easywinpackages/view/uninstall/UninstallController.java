package com.softeyt.easywinpackages.view.uninstall;

import com.google.inject.Inject;
import com.softeyt.easywinpackages.i18n.MessageService;
import com.softeyt.easywinpackages.model.Package;
import com.softeyt.easywinpackages.model.PackageSource;
import com.softeyt.easywinpackages.view.main.AdminWarningDialog;
import com.softeyt.easywinpackages.view.main.AlertHelper;
import com.softeyt.easywinpackages.view.main.Refreshable;
import com.softeyt.easywinpackages.viewmodel.UninstallViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;


public class UninstallController implements Initializable, Refreshable {

    @FXML private Label    lblTitle;
    @FXML private Label    lblSubtitle;
    @FXML private ComboBox<String> cmbSource;
    @FXML private Button   btnRefresh;
    @FXML private Label    lblStatus;
    @FXML private ProgressBar progressBar;
    @FXML private TableView<Package>            tblInstalled;
    @FXML private TableColumn<Package, String>  colId;
    @FXML private TableColumn<Package, String>  colName;
    @FXML private TableColumn<Package, String>  colVersion;
    @FXML private TableColumn<Package, String>  colSource;
    @FXML private TableColumn<Package, Void>    colAction;
    @FXML private Label    lblNoPackages;

    @Inject private UninstallViewModel    viewModel;
    @Inject private MessageService        messageService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblTitle.textProperty().bind(messageService.bind("uninstall.title"));
        lblSubtitle.textProperty().bind(messageService.bind("uninstall.subtitle"));
        btnRefresh.textProperty().bind(messageService.bind("dashboard.refresh"));
        lblStatus.textProperty().bind(viewModel.statusMessageProperty());
        lblNoPackages.textProperty().bind(messageService.bind("uninstall.empty"));

        progressBar.visibleProperty().bind(viewModel.busyProperty());
        progressBar.managedProperty().bind(viewModel.busyProperty());

        cmbSource.getItems().add("ALL");
        for (PackageSource s : PackageSource.values()) cmbSource.getItems().add(s.name());
        cmbSource.getSelectionModel().selectFirst();
        cmbSource.setOnAction(e -> loadWithFilter());

        colName.textProperty().bind(messageService.bind("uninstall.col.name"));
        colId.textProperty().bind(messageService.bind("uninstall.col.id"));
        colVersion.textProperty().bind(messageService.bind("uninstall.col.version"));
        colSource.textProperty().bind(messageService.bind("uninstall.col.source"));
        colAction.textProperty().bind(messageService.bind("uninstall.remove"));

        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name()));
        colId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().id()));
        colVersion.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().version()));
        colSource.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().source().name()));

        
        colName.setCellFactory(col -> createAlignedCell());
        colId.setCellFactory(col -> createAlignedCell());
        colVersion.setCellFactory(col -> createAlignedVersionCell());
        colSource.setCellFactory(col -> createSourceBadgeCell());

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();
            {
                btn.getStyleClass().add("btn-danger");
                btn.textProperty().bind(messageService.bind("uninstall.remove"));
                btn.setOnAction(e -> confirmUninstall(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });

        tblInstalled.setItems(viewModel.getPackages());
    }

    @Override public void onNavigatedTo() { loadWithFilter(); }

    @FXML private void onRefresh() { loadWithFilter(); }

    private void confirmUninstall(Package pkg) {
        boolean confirmed = AlertHelper.showConfirmation(
                tblInstalled.getScene(),
                messageService.getString("uninstall.confirm.title"),
                messageService.getString("uninstall.confirm.message", pkg.name()),
                messageService.getString("uninstall.remove"),
                messageService.getString("common.cancel"));
        if (!confirmed) return;
        viewModel.uninstall(pkg,
            () -> {},
            err -> AlertHelper.showError(tblInstalled.getScene(),
                    messageService.getString("common.error"), err));
    }

    private void loadWithFilter() {
        String filter = cmbSource.getValue();
        PackageSource source = "ALL".equals(filter) ? null : PackageSource.valueOf(filter);
        viewModel.loadInstalled(source);
    }

    

    private static TableCell<Package, String> createAlignedCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            }
        };
    }

    private static TableCell<Package, String> createAlignedVersionCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        };
    }

    private TableCell<Package, String> createSourceBadgeCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) { setGraphic(null); return; }
                javafx.scene.control.Label badge = new javafx.scene.control.Label(item);
                badge.getStyleClass().add("source-badge");
                badge.getStyleClass().add(switch (item) {
                    case "WINGET" -> "source-badge-winget";
                    case "CHOCO"  -> "source-badge-choco";
                    default       -> "source-badge-scoop";
                });
                setGraphic(badge);
                setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            }
        };
    }
}
