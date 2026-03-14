package com.softeyt.easywinpackages.viewmodel;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.softeyt.easywinpackages.theme.Theme;
import com.softeyt.easywinpackages.theme.ThemeManager;
import javafx.beans.property.*;


@Singleton
public class MainViewModel {

    private final ThemeManager themeManager;

    private final StringProperty  activeScreen  = new SimpleStringProperty("DASHBOARD");
    private final BooleanProperty darkThemeActive = new SimpleBooleanProperty(false);

    @Inject
    public MainViewModel(ThemeManager themeManager) {
        this.themeManager = themeManager;
    }

    
    public void navigateTo(String screen) {
        activeScreen.set(screen);
    }

    
    public void toggleTheme() {
        boolean toDark = !darkThemeActive.get();
        darkThemeActive.set(toDark);
        themeManager.switchTheme(toDark ? Theme.DARK : Theme.LIGHT);
    }

    public StringProperty  activeScreenProperty()   { return activeScreen; }
    public BooleanProperty darkThemeActiveProperty() { return darkThemeActive; }
}

