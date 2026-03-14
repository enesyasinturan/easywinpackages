package com.softeyt.easywinpackages.app;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;


public final class AppGlyphIcons {

    public static final Ikon NAV_DASHBOARD = FontAwesomeSolid.HOME;
    public static final Ikon NAV_INSTALL = FontAwesomeSolid.DOWNLOAD;
    public static final Ikon NAV_UPDATES = FontAwesomeSolid.SYNC_ALT;
    public static final Ikon NAV_UNINSTALL = FontAwesomeSolid.TRASH_ALT;
    public static final Ikon NAV_BUNDLE = FontAwesomeSolid.ARCHIVE;
    public static final Ikon NAV_SOURCES = FontAwesomeSolid.SERVER;
    public static final Ikon NAV_SETTINGS = FontAwesomeSolid.COG;
    public static final Ikon STATUS_SUCCESS = FontAwesomeSolid.CHECK_CIRCLE;
    public static final Ikon STATUS_BUNDLE_ADDED = FontAwesomeSolid.CHECK;
    public static final Ikon ACTION_BUNDLE_ADD = FontAwesomeSolid.PLUS;
    public static final Ikon ACTION_REMOVE = FontAwesomeSolid.TIMES;

    private AppGlyphIcons() {
    }

    
    public static Label createNavigationIcon(Ikon icon) {
        Label label = new Label();
        label.getStyleClass().add("nav-icon");
        label.setGraphic(createFontIcon(icon, "nav-icon-glyph", 18));
        return label;
    }

    
    public static StackPane createSuccessPlaceholderIcon() {
        FontIcon icon = createFontIcon(STATUS_SUCCESS, "success-icon-placeholder", 54);
        StackPane wrapper = new StackPane(icon);
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    
    public static FontIcon createRemoveGlyph() {
        FontIcon icon = createFontIcon(ACTION_REMOVE, "table-action-icon", 12);
        icon.setIconColor(Color.web("#f38ba8"));
        return icon;
    }

    
    public static FontIcon createBundleAddGlyph() {
        FontIcon icon = createFontIcon(ACTION_BUNDLE_ADD, "table-action-icon", 12);
        icon.setIconColor(Color.web("#89b4fa"));
        return icon;
    }

    
    public static FontIcon createBundleAddedGlyph() {
        FontIcon icon = createFontIcon(STATUS_BUNDLE_ADDED, "table-action-icon", 12);
        icon.setIconColor(Color.web("#22c55e"));
        return icon;
    }

    
    public static FontIcon createFontIcon(Ikon ikon, String styleClass, int size) {
        FontIcon icon = new FontIcon(ikon);
        icon.getStyleClass().add(styleClass);
        icon.setIconSize(size);
        return icon;
    }
}


