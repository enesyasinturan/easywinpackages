package com.softeyt.easywinpackages.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.softeyt.easywinpackages.model.Package;
import com.softeyt.easywinpackages.model.PackageSource;
import com.softeyt.easywinpackages.model.UpdateInfo;
import com.softeyt.easywinpackages.service.CommandExecutor;
import com.softeyt.easywinpackages.service.PackageManagerService;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


@Singleton
public class WingetService implements PackageManagerService {

    private static final Logger log = LoggerFactory.getLogger(WingetService.class);

    private final CommandExecutor executor;

    @Inject
    public WingetService(CommandExecutor executor) {
        this.executor = executor;
    }

    @Override
    public boolean isAvailable() {
        return executor.isCommandAvailable("winget");
    }

    @Override
    public String getDisplayName() {
        return "Winget";
    }

    @Override
    public Task<List<Package>> search(String query) {
        return new Task<>() {
            @Override
            protected List<Package> call() throws Exception {
                Task<String> cmd = executor.execute(
                        List.of("winget", "search", "--query", query,
                                "--accept-source-agreements", "--disable-interactivity"));
                cmd.run();
                String raw = cmd.get();
                log.debug("winget search raw ({} chars): {}", raw == null ? 0 : raw.length(),
                        raw == null ? "" : raw.substring(0, Math.min(120, raw.length()))
                                .replace("\n", "↵").replace("\r", ""));
                return parsePackageList(raw, false);
            }
        };
    }

    @Override
    public Task<Void> install(Package pkg) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<String> cmd = new ArrayList<>(List.of(
                        "winget", "install", "--id", pkg.id(),
                        "--accept-package-agreements", "--accept-source-agreements", "--silent"));
                if (pkg.version() != null) {
                    cmd.addAll(List.of("--version", pkg.version()));
                }
                Task<String> t = executor.execute(cmd);
                t.run();
                t.get();
                return null;
            }
        };
    }

    @Override
    public Task<Void> uninstall(Package pkg) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                Task<String> t = executor.execute(
                        List.of("winget", "uninstall", "--id", pkg.id(), "--silent"));
                t.run();
                t.get();
                return null;
            }
        };
    }

    @Override
    public Task<List<UpdateInfo>> getUpdates() {
        return new Task<>() {
            @Override
            protected List<UpdateInfo> call() throws Exception {
                Task<String> t = executor.execute(
                        List.of("winget", "upgrade", "--accept-source-agreements"));
                t.run();
                return parseUpdateList(t.get());
            }
        };
    }

    @Override
    public Task<Void> update(UpdateInfo update) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                Task<String> t = executor.execute(
                        List.of("winget", "upgrade", "--id", update.id(),
                                "--accept-package-agreements", "--accept-source-agreements", "--silent"));
                t.run();
                t.get();
                return null;
            }
        };
    }

    @Override
    public Task<Void> updateAll() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                Task<String> t = executor.execute(
                        List.of("winget", "upgrade", "--all",
                                "--accept-package-agreements", "--accept-source-agreements", "--silent"));
                t.run();
                t.get();
                return null;
            }
        };
    }

    @Override
    public Task<List<Package>> getInstalled() {
        return new Task<>() {
            @Override
            protected List<Package> call() throws Exception {
                Task<String> t = executor.execute(
                        List.of("winget", "list", "--accept-source-agreements"));
                t.run();
                return parsePackageList(t.get(), true);
            }
        };
    }

    @Override
    public Task<Integer> getInstalledCount() {
        return new Task<>() {
            @Override
            protected Integer call() throws Exception {
                Task<List<Package>> t = getInstalled();
                t.run();
                return t.get().size();
            }
        };
    }

    @Override
    public Task<Void> installManager() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                Path dependencyDirectory = resolveDependencyDirectory();
                String dependencyClause = buildDependencyClause(dependencyDirectory);
                String output = installFromLatestRelease(dependencyClause);
                if (!verifyInstallation()) {
                    Path localBundle = resolveBundledInstaller();
                    if (localBundle != null) {
                        log.warn("Winget still unavailable after latest-release install. Falling back to bundled installer: {}", localBundle);
                        output = output + "\n" + installFromBundledPackage(localBundle, dependencyClause);
                    }
                }
                if (!verifyInstallation()) {
                    throw new java.io.IOException("Winget installation completed but the executable could not be resolved afterward.");
                }
                log.info("Winget install output: {}", output);
                return null;
            }
        };
    }

    @Override
    public Task<Void> removeManager() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                String script = String.join("; ",
                        "$pkg=Get-AppxPackage -Name 'Microsoft.DesktopAppInstaller' -ErrorAction SilentlyContinue",
                        "if ($null -eq $pkg) { throw 'Winget is not installed.' }",
                        "$pkg | Remove-AppxPackage");
                Task<String> t = executor.powershell(script, 240);
                t.run();
                t.get();
                executor.refreshWingetPath();
                return null;
            }
        };
    }

    private Path resolveDependencyDirectory() {
        String architecture = System.getenv("PROCESSOR_ARCHITEW6432");
        if (architecture == null || architecture.isBlank()) {
            architecture = System.getenv("PROCESSOR_ARCHITECTURE");
        }
        String normalizedArchitecture;
        if (architecture != null && architecture.toLowerCase(java.util.Locale.ROOT).contains("arm")) {
            normalizedArchitecture = "arm64";
        } else if (architecture != null && architecture.contains("64")) {
            normalizedArchitecture = "x64";
        } else {
            normalizedArchitecture = "x86";
        }
        Path baseDirectory = Path.of(System.getProperty("user.dir"), "sources", "winget", "DesktopAppInstaller_Dependencies");
        Path candidate = baseDirectory.resolve(normalizedArchitecture);
        return Files.isDirectory(candidate) ? candidate : null;
    }

    private String buildDependencyClause(Path dependencyDirectory) {
        if (dependencyDirectory == null) {
            return "";
        }
        List<String> dependencyNames = List.of(
                findFirstExisting(dependencyDirectory, "Microsoft.VCLibs.140.00_", ".appx"),
                findFirstExisting(dependencyDirectory, "Microsoft.VCLibs.140.00.UWPDesktop", ".appx"),
                findFirstExisting(dependencyDirectory, "Microsoft.WindowsAppRuntime.", ".appx"));
        if (dependencyNames.stream().anyMatch(java.util.Objects::isNull)) {
            return "";
        }
        List<String> dependencies = dependencyNames.stream()
                .map(dependencyDirectory::resolve)
                .map(Path::toString)
                .toList();
        return " -DependencyPath @('" + dependencies.stream()
                .map(this::escapePowerShellLiteral)
                .reduce((left, right) -> left + "','" + right)
                .orElse("") + "')";
    }

    private String findFirstExisting(Path directory, String prefix, String suffix) {
        try (var stream = Files.list(directory)) {
            return stream
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.startsWith(prefix) && name.endsWith(suffix))
                    .findFirst()
                    .orElse(null);
        } catch (Exception ex) {
            log.debug("Failed to enumerate winget dependency directory {}: {}", directory, ex.getMessage());
            return null;
        }
    }

    private String escapePowerShellLiteral(String value) {
        return value.replace("'", "''");
    }

    private String installFromLatestRelease(String dependencyClause) throws Exception {
        String psCommand = String.join("; ",
                "$ProgressPreference='SilentlyContinue'",
                "$existingPackage=Get-AppxPackage -Name 'Microsoft.DesktopAppInstaller' -ErrorAction SilentlyContinue | Sort-Object Version -Descending | Select-Object -First 1",
                "if ($null -ne $existingPackage) { Write-Output ('Winget package already installed: ' + $existingPackage.Version); return }",
                "$releaseApi='https://api.github.com/repos/microsoft/winget-cli/releases/latest'",
                "$downloadUrl=(Invoke-WebRequest -Uri $releaseApi -UseBasicParsing).Content | ConvertFrom-Json | Select-Object -ExpandProperty assets | Where-Object { $_.browser_download_url -match '.msixbundle$' } | Select-Object -First 1 -ExpandProperty browser_download_url",
                "if ([string]::IsNullOrWhiteSpace($downloadUrl)) { throw 'Winget release asset could not be resolved.' }",
                "$setupPath=Join-Path $env:TEMP 'EasyWinPackages-Setup.msixbundle'",
                "try {",
                "  Invoke-WebRequest -Uri $downloadUrl -OutFile $setupPath -UseBasicParsing",
                "  Add-AppxPackage -Path $setupPath" + dependencyClause,
                "} finally {",
                "  if (Test-Path $setupPath) { Remove-Item $setupPath -Force }",
                "}");
        log.info("Installing Winget from latest GitHub release.");
        return runWingetInstallScript(psCommand, 600);
    }

    private String installFromBundledPackage(Path bundledPackage, String dependencyClause) throws Exception {
        String bundledPath = escapePowerShellLiteral(bundledPackage.toString());
        String psCommand = "Add-AppxPackage -Path '" + bundledPath + "'" + dependencyClause;
        return runWingetInstallScript(psCommand, 420);
    }

    private Path resolveBundledInstaller() {
        Path bundledPackage = Path.of(System.getProperty("user.dir"), "sources", "winget",
                "Microsoft.DesktopAppInstaller_8wekyb3d8bbwe.msixbundle");
        return Files.exists(bundledPackage) ? bundledPackage : null;
    }

    private boolean verifyInstallation() {
        for (int attempt = 0; attempt < 15; attempt++) {
            executor.refreshWingetPath();
            if (executor.isCommandAvailable("winget")) {
                return true;
            }
            try {
                Thread.sleep(2_000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return executor.isCommandAvailable("winget");
            }
        }
        return executor.isCommandAvailable("winget");
    }

    private String runWingetInstallScript(String script, int timeoutSeconds) throws Exception {
        try {
            Task<String> psTask = executor.powershell(script, timeoutSeconds);
            psTask.run();
            return psTask.get();
        } catch (Exception ex) {
            if (isBenignInstalledConflict(ex)) {
                log.info("Winget install skipped because DesktopAppInstaller is already installed at a higher or equal version.");
                return ex.getMessage() != null ? ex.getMessage() : "Winget package already installed.";
            }
            throw ex;
        }
    }

    private boolean isBenignInstalledConflict(Throwable throwable) {
        String message = throwable != null ? throwable.getMessage() : null;
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalizedMessage = message.toLowerCase(java.util.Locale.ROOT);
        return normalizedMessage.contains("0x80073d06")
                || (normalizedMessage.contains("higher version") && normalizedMessage.contains("already installed"));
    }

    

    
    private static String stripAnsi(String s) {
        if (s == null) return "";
        return s.replaceAll("\u001B\\[[;\\d]*[A-Za-z]", "")
                .replaceAll("\u001B[\\(\\)][A-B0-2]", "")
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
    }

    
    private static boolean isSeparatorLine(String line) {
        if (line == null || line.isBlank()) return false;
        return line.chars().allMatch(c -> c == '-' || c == '\u2500' || c == ' ' || c == '\t');
    }

    
    private List<Package> parsePackageList(String output, boolean installed) {
        List<Package> packages = new ArrayList<>();
        if (output == null || output.isBlank()) return packages;
        
        if (output.startsWith("The system cannot") || output.startsWith("ERROR")) {
            log.warn("winget returned error output: {}", output.substring(0, Math.min(80, output.length())));
            return packages;
        }

        String[] lines = output.split("\r?\n");
        int[] colStarts = null;
        boolean headerPassed = false;

        for (String raw : lines) {
            String line = stripAnsi(raw);

            if (!headerPassed && isSeparatorLine(line)) {
                colStarts = detectColumnStarts(line);
                headerPassed = true;
                continue;
            }
            if (!headerPassed || colStarts == null) continue;
            if (line.isBlank()) continue;
            if (line.contains("package") && (line.startsWith(" ") || line.startsWith("\t"))) continue;

            String[] cols = splitByOffsets(line, colStarts);
            if (cols.length >= 3) {
                String name    = cols[0].trim();
                String id      = cols[1].trim();
                String version = cols[2].trim();
                if (!id.isBlank() && !id.equalsIgnoreCase("Id")) {
                    packages.add(new Package(id, name.isEmpty() ? id : name, version, PackageSource.WINGET));
                }
            }
        }
        log.debug("Parsed {} packages from winget output ({} lines)", packages.size(), lines.length);
        return packages;
    }

    
    private List<UpdateInfo> parseUpdateList(String output) {
        List<UpdateInfo> updates = new ArrayList<>();
        if (output == null || output.isBlank()) return updates;
        if (output.startsWith("The system cannot") || output.startsWith("ERROR")) {
            log.warn("winget upgrade returned error: {}", output.substring(0, Math.min(80, output.length())));
            return updates;
        }

        String[] lines = output.split("\r?\n");
        int[] colStarts = null;
        boolean headerPassed = false;

        for (String raw : lines) {
            String line = stripAnsi(raw);

            if (!headerPassed && isSeparatorLine(line)) {
                colStarts = detectColumnStarts(line);
                headerPassed = true;
                continue;
            }
            if (!headerPassed || colStarts == null) continue;
            if (line.isBlank()) continue;
            if (line.contains("upgrade") && (line.startsWith(" ") || line.startsWith("\t"))) continue;

            String[] cols = splitByOffsets(line, colStarts);
            if (cols.length >= 4) {
                String name    = cols[0].trim();
                String id      = cols[1].trim();
                String current = cols[2].trim();
                String newVer  = cols[3].trim();
                if (!id.isBlank() && !newVer.isBlank()
                        && !newVer.equalsIgnoreCase("Available")
                        && !newVer.equalsIgnoreCase("Version")
                        && !id.equalsIgnoreCase("Id")) {
                    updates.add(new UpdateInfo(id, name.isEmpty() ? id : name,
                            current, newVer, PackageSource.WINGET));
                }
            }
        }
        log.debug("Parsed {} updates from winget output", updates.size());
        return updates;
    }

    
    private static int[] detectColumnStarts(String separatorLine) {
        List<Integer> starts = new ArrayList<>();
        boolean inDashes = false;
        for (int i = 0; i < separatorLine.length(); i++) {
            char c = separatorLine.charAt(i);
            boolean isDash = c == '-' || c == '\u2500';
            if (isDash && !inDashes) { starts.add(i); inDashes = true; }
            else if (!isDash)        { inDashes = false; }
        }
        return starts.stream().mapToInt(Integer::intValue).toArray();
    }

    
    private static String[] splitByOffsets(String line, int[] colStarts) {
        String[] result = new String[colStarts.length];
        for (int i = 0; i < colStarts.length; i++) {
            int start = colStarts[i];
            int end   = (i + 1 < colStarts.length) ? colStarts[i + 1] : line.length();
            if (start >= line.length()) { result[i] = ""; continue; }
            end = Math.min(end, line.length());
            result[i] = line.substring(start, end);
        }
        return result;
    }
}

