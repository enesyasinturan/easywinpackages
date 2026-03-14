package com.softeyt.easywinpackages.view.dashboard;

import com.google.inject.Inject;
import com.softeyt.easywinpackages.app.AppGlyphIcons;
import com.softeyt.easywinpackages.i18n.MessageService;
import com.softeyt.easywinpackages.service.impl.ChocoService;
import com.softeyt.easywinpackages.service.impl.ScoopService;
import com.softeyt.easywinpackages.service.impl.WingetService;
import com.softeyt.easywinpackages.view.main.MainController;
import com.softeyt.easywinpackages.view.main.Refreshable;
import com.softeyt.easywinpackages.viewmodel.DashboardViewModel;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ResourceBundle;


public class DashboardController implements Initializable, Refreshable {

    @FXML private Label             lblTitle;
    @FXML private Label             lblSubtitle;
    @FXML private Button            btnRefresh;

    
    @FXML private ProgressIndicator spinUpdates;
    @FXML private ProgressIndicator spinTotal;
    @FXML private Label             lblUpdatesTitle;
    @FXML private Label             lblUpdatesIcon;
    @FXML private Label             lblUpdatesCount;
    @FXML private HBox              boxUpdatesCurrent;
    @FXML private Label             lblUpdatesCurrentIcon;
    @FXML private Label             lblUpdatesCurrentText;
    @FXML private Label             lblUpdatesSub;
    @FXML private Label             lblTotalTitle;
    @FXML private Label             lblTotalIcon;
    @FXML private Label             lblTotalCount;
    @FXML private Label             lblTotalSub;
    @FXML private Label             lblSourcesTitle;
    @FXML private Label             lblSourcesIcon;
    @FXML private Label             lblSourcesCount;
    @FXML private Label             lblSourcesSub;

    @Inject private DashboardViewModel viewModel;
    @Inject private MainController     mainController;
    @Inject private MessageService     messageService;
    @Inject private WingetService      wingetService;
    @Inject private ChocoService       chocoService;
    @Inject private ScoopService       scoopService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblTitle.textProperty().bind(messageService.bind("dashboard.title"));
        lblSubtitle.textProperty().bind(messageService.bind("dashboard.subtitle"));
        btnRefresh.textProperty().bind(messageService.bind("dashboard.refresh"));

        lblUpdatesTitle.textProperty().bind(messageService.bind("dashboard.pending.updates"));
        lblUpdatesSub.textProperty().bind(messageService.bind("dashboard.updates.sub"));
        lblTotalTitle.textProperty().bind(messageService.bind("dashboard.total.installed"));
        lblTotalSub.textProperty().bind(messageService.bind("dashboard.source.winget"));
        lblSourcesTitle.textProperty().bind(messageService.bind("dashboard.available.sources"));
        lblSourcesSub.textProperty().bind(messageService.bind("dashboard.sources.status"));
        lblUpdatesCurrentText.textProperty().bind(messageService.bind("dashboard.updates.current"));

        configureMetricIcons();
        bindUpdatesCard();
        bindTotalCard();
    }

    @Override
    public void onNavigatedTo() {
        viewModel.refresh();
        refreshSourceStatus();
    }

    @FXML
    private void onRefresh() {
        viewModel.refresh();
        refreshSourceStatus();
    }

    @FXML
    private void onUpdatesCardClicked() {
        mainController.navigateToScreen("UPDATES");
    }

    @FXML
    private void onTotalCardClicked() {
        mainController.navigateToScreen("UNINSTALL");
    }

    @FXML
    private void onSourcesCardClicked() {
        mainController.navigateToScreen("SOURCES");
    }

    

    private void bindUpdatesCard() {
        spinUpdates.visibleProperty().bind(viewModel.updatesLoadingProperty());
        spinUpdates.managedProperty().bind(viewModel.updatesLoadingProperty());
        lblUpdatesCount.visibleProperty().bind(viewModel.updatesLoadingProperty().not().and(viewModel.updatesCountProperty().greaterThan(0)));
        lblUpdatesCount.managedProperty().bind(lblUpdatesCount.visibleProperty());
        boxUpdatesCurrent.visibleProperty().bind(viewModel.updatesLoadingProperty().not().and(viewModel.updatesCountProperty().isEqualTo(0)));
        boxUpdatesCurrent.managedProperty().bind(boxUpdatesCurrent.visibleProperty());

        viewModel.updatesCountProperty().addListener((obs, o, n) -> {
            lblUpdatesCount.textProperty().unbind();
            lblUpdatesCount.setText(n.intValue() <= 0 ? "" : String.valueOf(n.intValue()));
        });
        lblUpdatesCount.textProperty().bind(messageService.bind("dashboard.loading"));
    }

    private void bindTotalCard() {
        spinTotal.visibleProperty().bind(viewModel.totalLoadingProperty());
        spinTotal.managedProperty().bind(viewModel.totalLoadingProperty());
        lblTotalCount.visibleProperty().bind(viewModel.totalLoadingProperty().not());
        lblTotalCount.managedProperty().bind(viewModel.totalLoadingProperty().not());

        viewModel.totalInstalledProperty().addListener((obs, o, n) -> {
            lblTotalCount.textProperty().unbind();
            lblTotalCount.setText(n.intValue() < 0 ? "—" : String.valueOf(n.intValue()));
        });
        lblTotalCount.textProperty().bind(messageService.bind("dashboard.loading"));
    }

    private void refreshSourceStatus() {
        Thread.ofVirtual().start(() -> {
            boolean w = wingetService.isAvailable();
            boolean c = chocoService.isAvailable();
            boolean s = scoopService.isAvailable();
            int available = (w ? 1 : 0) + (c ? 1 : 0) + (s ? 1 : 0);
            javafx.application.Platform.runLater(() -> {
                lblSourcesCount.textProperty().unbind();
                lblSourcesCount.setText(String.valueOf(available) + " / 3");
            });
        });
    }

    private void configureMetricIcons() {
        lblUpdatesIcon.setGraphic(AppGlyphIcons.createFontIcon(AppGlyphIcons.NAV_UPDATES, "metric-icon-glyph", 18));
        lblTotalIcon.setGraphic(AppGlyphIcons.createFontIcon(AppGlyphIcons.NAV_INSTALL, "metric-icon-glyph", 18));
        lblSourcesIcon.setGraphic(AppGlyphIcons.createFontIcon(AppGlyphIcons.NAV_SOURCES, "metric-icon-glyph", 18));
        lblUpdatesCurrentIcon.setGraphic(AppGlyphIcons.createFontIcon(AppGlyphIcons.STATUS_SUCCESS, "updates-current-icon-glyph", 16));
    }
}
