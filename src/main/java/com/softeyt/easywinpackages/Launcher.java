package com.softeyt.easywinpackages;

import com.softeyt.easywinpackages.app.AppRuntimePaths;
import com.softeyt.easywinpackages.app.MainApplication;
import javafx.application.Application;


public class Launcher {
    public static void main(String[] args) {
        AppRuntimePaths.initialize(Launcher.class);
        Application.launch(MainApplication.class, args);
    }
}
