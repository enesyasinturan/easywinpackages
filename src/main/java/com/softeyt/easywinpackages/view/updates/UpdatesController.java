package com.softeyt.easywinpackages.view.updates;

import com.google.inject.Inject;
import com.softeyt.easywinpackages.app.AppGlyphIcons;
import com.softeyt.easywinpackages.i18n.MessageService;
import com.softeyt.easywinpackages.model.UpdateInfo;
import com.softeyt.easywinpackages.view.main.AdminWarningDialog;
import com.softeyt.easywinpackages.view.main.AlertHelper;
import com.softeyt.easywinpackages.view.main.Refreshable;
import com.softeyt.easywinpackages.viewmodel.UpdatesViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ResourceBundle;
import java.net.URL;


public class UpdatesController implements Initializable, Refreshable {

    @FXML private Label     lblTitle;
    @FXML private Label     lblSubtitle;
    @FXML private Button    btnRefresh;
    @FXML private Button    btnUpdateAll;
    @FXML private Label     lblStatus;
    @FXML private ProgressBar progressBar;
    @FXML private TableView<UpdateInfo>              tblUpdates;
    @FXML private TableColumn<UpdateInfo, Boolean>   colSelect;
    @FXML private TableColumn<UpdateInfo, String>    colName;
    @FXML private TableColumn<UpdateInfo, String>    colCurrent;
    @FXML private TableColumn<UpdateInfo, String>    colNew;
    @FXML private TableColumn<UpdateInfo, String>    colSource;
    @FXML private TableColumn<UpdateInfo, Void>      colAction;
    @FXML private Label     lblNoUpdates;

    @Inject private UpdatesViewModel      viewModel;
    @Inject private MessageService        messageService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblTitle.textProperty().bind(messageService.bind("updates.title"));
        lblSubtitle.textProperty().bind(messageService.bind("updates.subtitle"));
        btnRefresh.textProperty().bind(messageService.bind("dashboard.refresh"));
        btnUpdateAll.textProperty().bind(messageService.bind("updates.update.all"));
        lblStatus.textProperty().bind(viewModel.statusMessageProperty());
        lblNoUpdates.textProperty().bind(messageService.bind("updates.no.updates"));

        progressBar.visibleProperty().bind(viewModel.busyProperty());
        progressBar.managedProperty().bind(viewModel.busyProperty());
        btnUpdateAll.disableProperty().bind(viewModel.busyProperty());

        colSelect.setCellValueFactory(d -> new javafx.beans.property.SimpleBooleanProperty(false));
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        colSelect.setStyle("-fx-alignment: CENTER;");
        colSelect.setEditable(true);
        tblUpdates.setEditable(true);

        colName.textProperty().bind(messageService.bind("updates.col.name"));
        colCurrent.textProperty().bind(messageService.bind("updates.col.current"));
        colNew.textProperty().bind(messageService.bind("updates.col.new"));
        colSource.textProperty().bind(messageService.bind("updates.col.source"));
        colAction.textProperty().bind(messageService.bind("updates.update.one"));

        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name()));
        colCurrent.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().currentVersion()));
        colNew.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().newVersion()));
        colSource.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().source().name()));

        colNew.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                setText(item);
                getStyleClass().removeAll("badge-warning");
                getStyleClass().add("badge-warning");
            }
        });

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();
            {
                btn.getStyleClass().add("btn-secondary");
                btn.textProperty().bind(messageService.bind("updates.update.one"));
                btn.setOnAction(e -> {
                    UpdateInfo u = getTableView().getItems().get(getIndex());
                    viewModel.update(u,
                        () -> {}, err -> AlertHelper.showError(tblUpdates.getScene(),
                                messageService.getString("common.error"), err));
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });

        tblUpdates.setItems(viewModel.getUpdates());

        
        tblUpdates.setPlaceholder(buildUpToDatePlaceholder());
    }

    @Override public void onNavigatedTo() { viewModel.loadUpdates(); }

    @FXML private void onRefresh()   { viewModel.loadUpdates(); }
    @FXML private void onUpdateAll() {
        viewModel.updateAll(
            () -> {}, err -> AlertHelper.showError(tblUpdates.getScene(),
                    messageService.getString("common.error"), err));
    }

    

    private javafx.scene.Node buildUpToDatePlaceholder() {
        VBox box = new VBox(14);
        box.setAlignment(javafx.geometry.Pos.CENTER);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(56, 56);
        spinner.visibleProperty().bind(viewModel.busyProperty());
        spinner.managedProperty().bind(viewModel.busyProperty());

        StackPane iconContainer = new StackPane();
        iconContainer.getChildren().add(spinner);

        javafx.scene.Node successIcon = AppGlyphIcons.createSuccessPlaceholderIcon();
        successIcon.visibleProperty().bind(viewModel.busyProperty().not());
        successIcon.managedProperty().bind(viewModel.busyProperty().not());
        iconContainer.getChildren().add(successIcon);
        box.getChildren().add(iconContainer);

        Label lbl = new Label();
        lbl.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> viewModel.busyProperty().get()
                        ? messageService.getString("updates.progress.running")
                        : messageService.getString("updates.no.updates"),
                viewModel.busyProperty(), messageService.localeProperty()));
        lbl.getStyleClass().add("muted-label");
        box.getChildren().add(lbl);
        return box;
    }
}
