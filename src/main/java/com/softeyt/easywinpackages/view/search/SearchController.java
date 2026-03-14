package com.softeyt.easywinpackages.view.search;

import com.google.inject.Inject;
import com.softeyt.easywinpackages.app.AppGlyphIcons;
import com.softeyt.easywinpackages.i18n.MessageService;
import com.softeyt.easywinpackages.model.BundleEntry;
import com.softeyt.easywinpackages.model.Package;
import com.softeyt.easywinpackages.model.PackageSource;
import com.softeyt.easywinpackages.view.main.AdminWarningDialog;
import com.softeyt.easywinpackages.view.main.AlertHelper;
import com.softeyt.easywinpackages.viewmodel.BundleViewModel;
import com.softeyt.easywinpackages.viewmodel.SearchViewModel;
import com.softeyt.easywinpackages.viewmodel.SourcesViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;


public class SearchController implements Initializable {

    
    private static final String ALL = "ALL";

    @FXML private Label            lblTitle;
    @FXML private Label            lblSubtitle;
    @FXML private ComboBox<String> cmbSource;
    @FXML private TextField        txtSearch;
    @FXML private Button           btnSearch;
    @FXML private Label            lblStatus;
    @FXML private ProgressBar      progressBar;
    @FXML private TableView<Package>           tblResults;
    @FXML private TableColumn<Package, String> colId;
    @FXML private TableColumn<Package, String> colName;
    @FXML private TableColumn<Package, String> colVersion;
    @FXML private TableColumn<Package, String> colSource;
    @FXML private TableColumn<Package, Void>   colAction;
    @FXML private TableColumn<Package, Void>   colBundle;
    @FXML private Label            lblNoResults;

    @Inject private SearchViewModel       viewModel;
    @Inject private MessageService        messageService;
    @Inject private SourcesViewModel      sourcesViewModel;
    @Inject private BundleViewModel       bundleViewModel;

    
    private String lastValidSource = ALL;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblTitle.textProperty().bind(messageService.bind("search.title"));
        lblSubtitle.textProperty().bind(messageService.bind("search.subtitle"));
        btnSearch.textProperty().bind(messageService.bind("search.button"));
        txtSearch.promptTextProperty().bind(messageService.bind("search.placeholder"));
        lblStatus.textProperty().bind(viewModel.statusMessageProperty());
        lblNoResults.textProperty().bind(messageService.bind("search.no.results"));

        progressBar.visibleProperty().bind(viewModel.busyProperty());
        progressBar.managedProperty().bind(viewModel.busyProperty());
        btnSearch.disableProperty().bind(viewModel.busyProperty());

        txtSearch.setOnAction(e -> onSearch());

        
        cmbSource.getItems().add(ALL);
        for (PackageSource s : PackageSource.values()) cmbSource.getItems().add(s.name());
        cmbSource.getSelectionModel().selectFirst();   

        
        cmbSource.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(String v) {
                if (ALL.equals(v)) return messageService.getString("search.source.all");
                return v;
            }
            @Override public String fromString(String s) { return s; }
        });

        cmbSource.setOnAction(e -> onSourceChanged());

        
        colId.textProperty().bind(messageService.bind("search.col.id"));
        colName.textProperty().bind(messageService.bind("search.col.name"));
        colVersion.textProperty().bind(messageService.bind("search.col.version"));
        colSource.textProperty().bind(messageService.bind("search.col.source"));
        colAction.textProperty().bind(messageService.bind("search.install"));
        colBundle.textProperty().bind(messageService.bind("search.col.bundle"));

        colId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().id()));
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name()));
        colVersion.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().version()));
        colSource.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().source().name()));

        
        colSource.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item);
                badge.getStyleClass().add("source-badge");
                badge.getStyleClass().add(switch (item) {
                    case "WINGET" -> "source-badge-winget";
                    case "CHOCO"  -> "source-badge-choco";
                    default       -> "source-badge-scoop";
                });
                setGraphic(badge);
            }
        });

        
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnInstall = new Button();
            {
                btnInstall.setOnAction(e -> {
                    Package pkg = getTableView().getItems().get(getIndex());
                    if (viewModel.isInstalled(pkg) || viewModel.isInstalling(pkg)) {
                        return;
                    }
                    viewModel.install(pkg,
                            tblResults::refresh,
                            err -> AlertHelper.showError(tblResults.getScene(),
                                    messageService.getString("common.error"), err));
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    setAlignment(javafx.geometry.Pos.CENTER);
                    return;
                }
                Package pkg = getTableView().getItems().get(getIndex());
                boolean installed = viewModel.isInstalled(pkg);
                boolean installing = viewModel.isInstalling(pkg);

                btnInstall.getStyleClass().setAll("button", installed ? "btn-secondary" : "btn-primary");
                btnInstall.setDisable(installed || installing || viewModel.busyProperty().get());
                btnInstall.setText(installing
                        ? messageService.getString("search.installing")
                        : installed
                        ? messageService.getString("search.installed")
                        : messageService.getString("search.install"));
                setGraphic(btnInstall);
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });

        
        colBundle.setCellFactory(col -> new TableCell<>() {
            private final Button btnAdd = new Button();
            {
                btnAdd.setOnAction(e -> {
                    Package pkg = getTableView().getItems().get(getIndex());
                    if (bundleViewModel.containsEntryForPackage(pkg)) {
                        return;
                    }
                    bundleViewModel.addEntry(new BundleEntry(pkg.id(), pkg.name(),
                            pkg.source(), pkg.version()));
                    tblResults.refresh();
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    setAlignment(javafx.geometry.Pos.CENTER);
                    return;
                }
                Package pkg = getTableView().getItems().get(getIndex());
                boolean alreadyInBundle = bundleViewModel.containsEntryForPackage(pkg);

                btnAdd.getStyleClass().setAll("button", "btn-secondary");
                btnAdd.setDisable(alreadyInBundle);
                btnAdd.setText(alreadyInBundle
                        ? messageService.getString("search.bundle.added")
                        : messageService.getString("search.add.bundle"));
                btnAdd.setGraphic(alreadyInBundle
                        ? AppGlyphIcons.createBundleAddedGlyph()
                        : AppGlyphIcons.createBundleAddGlyph());
                setGraphic(btnAdd);
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });

        tblResults.setItems(viewModel.getResults());
        viewModel.stateRevisionProperty().addListener((obs, oldValue, newValue) -> tblResults.refresh());
        bundleViewModel.getEntries().addListener((ListChangeListener<BundleEntry>) change -> tblResults.refresh());
    }

    
    private void onSourceChanged() {
        String selected = cmbSource.getValue();
        if (selected == null || ALL.equals(selected)) {
            lastValidSource = ALL;
            return;
        }
        PackageSource src = PackageSource.valueOf(selected);
        boolean available = switch (src) {
            case WINGET -> sourcesViewModel.wingetAvailableProperty().get();
            case CHOCO  -> sourcesViewModel.chocoAvailableProperty().get();
            case SCOOP  -> sourcesViewModel.scoopAvailableProperty().get();
        };
        if (!available) {
            
            cmbSource.setOnAction(null);
            cmbSource.getSelectionModel().select(lastValidSource);
            cmbSource.setOnAction(e -> onSourceChanged());
            viewModel.statusMessageProperty().set(messageService.getString("search.source.unavailable.revert", selected));
        } else {
            lastValidSource = selected;
        }
    }

    @FXML
    private void onSearch() {
        String query = txtSearch.getText();
        if (query == null || query.isBlank()) return;
        String src = cmbSource.getValue();
        if (ALL.equals(src) || src == null) {
            viewModel.search(query, null);
        } else {
            viewModel.search(query, PackageSource.valueOf(src));
        }
    }
}

