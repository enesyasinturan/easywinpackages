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

import java.util.ArrayList;
import java.util.List;


@Singleton
public class ChocoService implements PackageManagerService {

    private static final Logger log = LoggerFactory.getLogger(ChocoService.class);

    private final CommandExecutor executor;

    @Inject
    public ChocoService(CommandExecutor executor) {
        this.executor = executor;
    }

    @Override
    public boolean isAvailable() {
        return executor.isCommandAvailable("choco");
    }

    @Override
    public String getDisplayName() {
        return "Chocolatey";
    }

    @Override
    public Task<List<Package>> search(String query) {
        return new Task<>() {
            @Override
            protected List<Package> call() throws Exception {
                Task<String> t = executor.execute(
                        List.of("choco", "search", query, "--limit-output"));
                t.run();
                return parseChocoList(t.get());
            }
        };
    }

    @Override
    public Task<Void> install(Package pkg) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<String> cmd = new ArrayList<>(
                        List.of("choco", "install", pkg.id(), "-y", "--no-progress"));
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
                        List.of("choco", "uninstall", pkg.id(), "-y", "--no-progress"));
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
                        List.of("choco", "outdated", "--limit-output"));
                t.run();
                return parseOutdated(t.get());
            }
        };
    }

    @Override
    public Task<Void> update(UpdateInfo update) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                Task<String> t = executor.execute(
                        List.of("choco", "upgrade", update.id(), "-y", "--no-progress"));
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
                        List.of("choco", "upgrade", "all", "-y", "--no-progress"));
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
                        List.of("choco", "list", "--limit-output", "--local-only"));
                t.run();
                return parseChocoList(t.get());
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
                
                String script = "Set-ExecutionPolicy Bypass -Scope Process -Force; " +
                        "[System.Net.ServicePointManager]::SecurityProtocol = " +
                        "[System.Net.ServicePointManager]::SecurityProtocol -bor 3072; " +
                        "iex ((New-Object System.Net.WebClient).DownloadString(" +
                        "'https://community.chocolatey.org/install.ps1'))";
                Task<String> t = executor.powershell(script);
                t.run();
                t.get();
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
                        "$pathsToRemove=@('C:\\ProgramData\\chocolatey\\bin','C:\\ProgramData\\chocolatey')",
                        "$userPath=[Environment]::GetEnvironmentVariable('Path','User')",
                        "$machinePath=[Environment]::GetEnvironmentVariable('Path','Machine')",
                        "$cleanPath={ param($pathValue) (($pathValue -split ';') | Where-Object { $_ -and ($pathsToRemove -notcontains $_) }) -join ';' }",
                        "[Environment]::SetEnvironmentVariable('Path', (& $cleanPath $userPath), 'User')",
                        "[Environment]::SetEnvironmentVariable('Path', (& $cleanPath $machinePath), 'Machine')",
                        "if (Test-Path 'C:\\ProgramData\\chocolatey') { Remove-Item 'C:\\ProgramData\\chocolatey' -Recurse -Force }");
                Task<String> t = executor.powershell(script, 240);
                t.run();
                t.get();
                return null;
            }
        };
    }

    

    private List<Package> parseChocoList(String output) {
        List<Package> packages = new ArrayList<>();
        if (output == null || output.isBlank()) return packages;

        for (String line : output.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) continue;
            
            String[] parts = trimmed.split("\\|");
            if (parts.length >= 2) {
                String id      = parts[0].trim();
                String version = parts[1].trim();
                packages.add(new Package(id, id, version, PackageSource.CHOCO));
            }
        }
        return packages;
    }

    private List<UpdateInfo> parseOutdated(String output) {
        List<UpdateInfo> updates = new ArrayList<>();
        if (output == null || output.isBlank()) return updates;

        for (String line : output.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) continue;
            
            String[] parts = trimmed.split("\\|");
            if (parts.length >= 3) {
                String id      = parts[0].trim();
                String current = parts[1].trim();
                String newVer  = parts[2].trim();
                updates.add(new UpdateInfo(id, id, current, newVer, PackageSource.CHOCO));
            }
        }
        return updates;
    }
}

