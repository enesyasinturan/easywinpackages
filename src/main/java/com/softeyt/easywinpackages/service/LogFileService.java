package com.softeyt.easywinpackages.service;

import com.google.inject.Singleton;
import com.softeyt.easywinpackages.app.AppRuntimePaths;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


@Singleton
public class LogFileService {

    
    public Path getLogsDirectory() {
        return AppRuntimePaths.getLogsDirectory();
    }

    
    public Path getCurrentLogFile() {
        return AppRuntimePaths.getCurrentLogFile();
    }

    
    public void openLogsDirectory() throws IOException {
        Path logsDirectory = getLogsDirectory();
        Files.createDirectories(logsDirectory);
        open(logsDirectory);
    }

    
    public void openCurrentLogFile() throws IOException {
        Path logFile = getCurrentLogFile();
        Path parent = logFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (Files.notExists(logFile)) {
            Files.createFile(logFile);
        }
        open(logFile);
    }

    private void open(Path path) throws IOException {
        if (!Desktop.isDesktopSupported()) {
            throw new IOException("Desktop integration is not available on this system.");
        }
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.OPEN)) {
            throw new IOException("Open action is not supported on this system.");
        }
        desktop.open(path.toFile());
    }
}

