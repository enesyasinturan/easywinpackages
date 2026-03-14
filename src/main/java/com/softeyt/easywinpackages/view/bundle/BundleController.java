package com.softeyt.easywinpackages.view.bundle;

import com.google.inject.Inject;
import com.softeyt.easywinpackages.app.AppGlyphIcons;
import com.softeyt.easywinpackages.i18n.MessageService;
import com.softeyt.easywinpackages.model.BundleEntry;
import com.softeyt.easywinpackages.model.PackageSource;
import com.softeyt.easywinpackages.view.main.AlertHelper;
import com.softeyt.easywinpackages.viewmodel.BundleViewModel;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;


public class BundleController implements Initializable {

    private static final DateTimeFormatter EXPORT_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    @FXML private Label    lblTitle;
    @FXML private Label    lblSubtitle;
    @FXML private TextField txtBundleName;
    @FXML private Button   btnExport;
    @FXML private Button   btnImport;
    @FXML private Button   btnInstallAll;
    @FXML private Label    lblStatus;
    @FXML private ProgressBar progressBar;
    @FXML private TextField txtAddId;
    @FXML private TextField txtAddName;
    @FXML private TextField txtAddVersion;
    @FXML private ComboBox<String> cmbAddSource;
    @FXML private Button   btnAddEntry;
    @FXML private TableView<BundleEntry>             tblEntries;
    @FXML private TableColumn<BundleEntry, String>   colId;
    @FXML private TableColumn<BundleEntry, String>   colName;
    @FXML private TableColumn<BundleEntry, String>   colVersion;
    @FXML private TableColumn<BundleEntry, String>   colSource;
    @FXML private TableColumn<BundleEntry, Void>     colRemove;

    @Inject private BundleViewModel viewModel;
    @Inject private MessageService  messageService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblTitle.textProperty().bind(messageService.bind("bundle.title"));
        lblSubtitle.textProperty().bind(messageService.bind("bundle.subtitle"));
        btnExport.textProperty().bind(messageService.bind("bundle.export"));
        btnImport.textProperty().bind(messageService.bind("bundle.import"));
        btnInstallAll.textProperty().bind(messageService.bind("bundle.install.all"));
        btnAddEntry.textProperty().bind(messageService.bind("bundle.add"));
        lblStatus.textProperty().bind(viewModel.statusMessageProperty());
        txtBundleName.textProperty().bindBidirectional(viewModel.bundleNameProperty());
        txtBundleName.promptTextProperty().bind(messageService.bind("bundle.name.placeholder"));
        txtAddId.promptTextProperty().bind(messageService.bind("bundle.col.id"));
        txtAddName.promptTextProperty().bind(messageService.bind("bundle.col.name"));
        txtAddVersion.promptTextProperty().bind(messageService.bind("bundle.col.version"));

        progressBar.visibleProperty().bind(viewModel.busyProperty());
        progressBar.managedProperty().bind(viewModel.busyProperty());
        progressBar.progressProperty().bind(viewModel.progressProperty());
        btnInstallAll.disableProperty().bind(viewModel.busyProperty());

        for (PackageSource s : PackageSource.values()) cmbAddSource.getItems().add(s.name());
        cmbAddSource.getSelectionModel().selectFirst();

        colId.textProperty().bind(messageService.bind("bundle.col.id"));
        colName.textProperty().bind(messageService.bind("bundle.col.name"));
        colVersion.textProperty().bind(messageService.bind("bundle.col.version"));
        colSource.textProperty().bind(messageService.bind("bundle.col.source"));
        colRemove.textProperty().bind(messageService.bind("bundle.remove"));

        colId.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().id()));
        colName.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().name()));
        colVersion.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().version() != null ? d.getValue().version() : ""));
        colSource.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().source().name()));
        colRemove.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();
            {
                btn.getStyleClass().add("btn-danger");
                btn.setGraphic(AppGlyphIcons.createRemoveGlyph());
                btn.setOnAction(e -> viewModel.removeEntry(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });

        tblEntries.setItems(viewModel.getEntries());
    }

    @FXML
    private void onAddEntry() {
        String id      = txtAddId.getText().trim();
        String name    = txtAddName.getText().trim();
        String version = txtAddVersion.getText().trim();
        String srcStr  = cmbAddSource.getValue();
        if (id.isEmpty() || srcStr == null) return;

        BundleEntry entry = new BundleEntry(id, name.isEmpty() ? id : name,
                PackageSource.valueOf(srcStr), version.isEmpty() ? null : version);
        viewModel.addEntry(entry);
        txtAddId.clear(); txtAddName.clear(); txtAddVersion.clear();
    }

    @FXML
    private void onExport() {
        FileChooser fc = new FileChooser();
        fc.setTitle(messageService.getString("bundle.export"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("EasyWinPackages Bundle", "*.ewpkg"));
        fc.setInitialFileName(buildDefaultExportFileName());
        File file = fc.showSaveDialog(tblEntries.getScene().getWindow());
        if (file == null) return;
        File normalizedFile = ensureBundleExtension(file);
        viewModel.export(normalizedFile.toPath(),
                () -> showInfo(messageService.getString("bundle.export.success")),
                err -> showError(err));
    }

    @FXML
    private void onImport() {
        FileChooser fc = new FileChooser();
        fc.setTitle(messageService.getString("bundle.import"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("EasyWinPackages Bundle", "*.ewpkg"));
        File file = fc.showOpenDialog(tblEntries.getScene().getWindow());
        if (file == null) return;
        viewModel.importBundle(file.toPath(),
                missing -> showWarning(messageService.getString("bundle.missing.sources",
                        String.join(", ", missing))),
                () -> showInfo(messageService.getString("bundle.import.success")),
                err -> showError(err));
    }

    @FXML
    private void onInstallAll() {
        viewModel.installAll(
                () -> showInfo(messageService.getString("common.success")),
                err -> showError(err));
    }

    private void showInfo(String msg) {
        AlertHelper.showInfo(tblEntries.getScene(), messageService.getString("common.success"), msg);
    }

    private void showWarning(String msg) {
        AlertHelper.showWarning(tblEntries.getScene(), messageService.getString("common.warning"), msg);
    }

    private void showError(String msg) {
        AlertHelper.showError(tblEntries.getScene(), messageService.getString("common.error"), msg);
    }

    private String buildDefaultExportFileName() {
        String rawBundleName = txtBundleName.getText() != null ? txtBundleName.getText().trim() : "";
        String safeName = rawBundleName
                .replaceAll("[\\\\/:*?\"<>|]", "-")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
        if (safeName.isBlank()) {
            safeName = "easywinpackages-bundle-" + LocalDateTime.now().format(EXPORT_NAME_FORMATTER);
        }
        return safeName + ".ewpkg";
    }

    private File ensureBundleExtension(File file) {
        if (file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".ewpkg")) {
            return file;
        }
        return new File(file.getParentFile(), file.getName() + ".ewpkg");
    }
}

