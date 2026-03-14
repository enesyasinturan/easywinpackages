package com.softeyt.easywinpackages.app;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;


public final class AppRuntimePaths {

    public static final String LOGS_DIR_PROPERTY = "easywinpackages.logs.dir";
    public static final String LOG_FILE_PROPERTY = "easywinpackages.log.file";
    public static final String INSTANCE_ID_PROPERTY = "easywinpackages.instance.id";

    private static final DateTimeFormatter INSTANCE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final Object INITIALIZATION_LOCK = new Object();

    private static volatile boolean initialized;
    private static Path applicationBaseDirectory;
    private static Path logsDirectory;
    private static Path currentLogFile;

    private AppRuntimePaths() {
    }

    
    public static void initialize(Class<?> anchorClass) {
        Objects.requireNonNull(anchorClass, "anchorClass must not be null");
        if (initialized) {
            return;
        }
        synchronized (INITIALIZATION_LOCK) {
            if (initialized) {
                return;
            }

            Path baseDirectory = resolveApplicationBaseDirectory(anchorClass);
            Path preferredLogsDirectory = baseDirectory.resolve("logs").normalize();
            Path resolvedLogsDirectory = prepareLogsDirectory(preferredLogsDirectory);
            String instanceId = buildInstanceId();
            Path resolvedLogFile = resolvedLogsDirectory.resolve("easywinpackages-" + instanceId + ".log").normalize();

            applicationBaseDirectory = baseDirectory;
            logsDirectory = resolvedLogsDirectory;
            currentLogFile = resolvedLogFile;

            System.setProperty(LOGS_DIR_PROPERTY, logsDirectory.toString());
            System.setProperty(LOG_FILE_PROPERTY, currentLogFile.toString());
            System.setProperty(INSTANCE_ID_PROPERTY, instanceId);

            initialized = true;
        }
    }

    
    public static Path getApplicationBaseDirectory() {
        ensureInitialized();
        return applicationBaseDirectory;
    }

    
    public static Path getLogsDirectory() {
        ensureInitialized();
        return logsDirectory;
    }

    
    public static Path getCurrentLogFile() {
        ensureInitialized();
        return currentLogFile;
    }

    static Path resolveApplicationBaseDirectory(Class<?> anchorClass) {
        return resolveApplicationBaseDirectory(resolveCodeSourcePath(anchorClass));
    }

    static Path resolveApplicationBaseDirectory(Path codeSourcePath) {
        Path normalizedPath = Objects.requireNonNull(codeSourcePath, "codeSourcePath must not be null")
                .toAbsolutePath()
                .normalize();
        if (Files.isRegularFile(normalizedPath) || looksLikePackagedArtifact(normalizedPath)) {
            return normalizedPath.getParent();
        }
        Path fileName = normalizedPath.getFileName();
        Path parent = normalizedPath.getParent();
        if (fileName != null
                && parent != null
                && "classes".equalsIgnoreCase(fileName.toString())
                && parent.getFileName() != null
                && "target".equalsIgnoreCase(parent.getFileName().toString())
                && parent.getParent() != null) {
            return parent.getParent();
        }
        return normalizedPath;
    }

    private static boolean looksLikePackagedArtifact(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            return false;
        }
        String lowerCaseName = fileName.toString().toLowerCase();
        return lowerCaseName.endsWith(".jar")
                || lowerCaseName.endsWith(".exe")
                || lowerCaseName.endsWith(".msi");
    }

    private static Path resolveCodeSourcePath(Class<?> anchorClass) {
        try {
            URL location = anchorClass.getProtectionDomain().getCodeSource().getLocation();
            return Path.of(location.toURI());
        } catch (URISyntaxException | NullPointerException ex) {
            return Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        }
    }

    private static Path prepareLogsDirectory(Path preferredLogsDirectory) {
        try {
            Files.createDirectories(preferredLogsDirectory);
            if (Files.isWritable(preferredLogsDirectory)) {
                return preferredLogsDirectory;
            }
        } catch (IOException ignored) {
            
        }

        Path fallbackDirectory = resolveFallbackLogsDirectory();
        try {
            Files.createDirectories(fallbackDirectory);
            return fallbackDirectory;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create a writable logs directory.", ex);
        }
    }

    private static Path resolveFallbackLogsDirectory() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Path.of(appData, "EasyWinPackages", "logs").toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.home", "."), "EasyWinPackages", "logs")
                .toAbsolutePath()
                .normalize();
    }

    private static String buildInstanceId() {
        return INSTANCE_FORMATTER.format(LocalDateTime.now()) + "-pid" + ProcessHandle.current().pid();
    }

    private static void ensureInitialized() {
        if (!initialized) {
            initialize(AppRuntimePaths.class);
        }
    }
}

