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
public class ScoopService implements PackageManagerService {

    private static final Logger log = LoggerFactory.getLogger(ScoopService.class);

    private final CommandExecutor executor;

    @Inject
    public ScoopService(CommandExecutor executor) {
        this.executor = executor;
    }

    @Override
    public boolean isAvailable() {
        return executor.isScoopAvailable();
    }

    @Override
    public String getDisplayName() {
        return "Scoop";
    }

    @Override
    public Task<List<Package>> search(String query) {
        return new Task<>() {
            @Override
            protected List<Package> call() throws Exception {
                Task<String> t = executor.powershell("scoop search " + query);
                t.run();
                return parseScoopSearch(t.get());
            }
        };
    }

    @Override
    public Task<Void> install(Package pkg) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                String cmd = pkg.version() != null
                        ? "scoop install " + pkg.id() + "@" + pkg.version()
                        : "scoop install " + pkg.id();
                Task<String> t = executor.powershell(cmd);
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
                Task<String> t = executor.powershell("scoop uninstall " + pkg.id());
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
                Task<String> t = executor.powershell("scoop status");
                t.run();
                return parseScoopStatus(t.get());
            }
        };
    }

    @Override
    public Task<Void> update(UpdateInfo update) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                Task<String> t = executor.powershell("scoop update " + update.id());
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
                Task<String> t = executor.powershell("scoop update *");
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
                Task<String> t = executor.powershell("scoop list");
                t.run();
                return parseScoopList(t.get());
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
                String script = String.join("; ",
                        "$ProgressPreference='SilentlyContinue'",
                        "Set-ExecutionPolicy RemoteSigned -Scope CurrentUser -Force",
                        "Invoke-Expression (New-Object System.Net.WebClient).DownloadString('https://get.scoop.sh')");
                Task<String> t = executor.powershell(script, 420);
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
                        "$scoopRoot=Join-Path $env:USERPROFILE 'scoop'",
                        "$pathsToRemove=@((Join-Path $env:USERPROFILE 'scoop\\shims'),$scoopRoot)",
                        "$userPath=[Environment]::GetEnvironmentVariable('Path','User')",
                        "$cleaned=(($userPath -split ';') | Where-Object { $_ -and ($pathsToRemove -notcontains $_) }) -join ';'",
                        "[Environment]::SetEnvironmentVariable('Path',$cleaned,'User')",
                        "if (Test-Path $scoopRoot) { Remove-Item $scoopRoot -Recurse -Force }");
                Task<String> t = executor.powershell(script, 240);
                t.run();
                t.get();
                return null;
            }
        };
    }

    

    private List<Package> parseScoopSearch(String output) {
        List<Package> packages = new ArrayList<>();
        if (output == null || output.isBlank()) return packages;

        boolean inResults = false;
        for (String raw : output.split("\n")) {
            String line = raw.trim();
            if (line.startsWith("----")) { inResults = true; continue; }
            if (!inResults || line.isBlank()) continue;

            
            String[] parts = line.split("\\s+");
            if (parts.length >= 2) {
                String name    = parts[0].trim();
                String version = parts[1].replaceAll("[()]", "").trim();
                packages.add(new Package(name, name, version, PackageSource.SCOOP));
            } else if (parts.length == 1) {
                packages.add(new Package(parts[0].trim(), parts[0].trim(), null, PackageSource.SCOOP));
            }
        }
        return packages;
    }

    private List<Package> parseScoopList(String output) {
        List<Package> packages = new ArrayList<>();
        if (output == null || output.isBlank()) return packages;

        boolean inResults = false;
        for (String raw : output.split("\n")) {
            String line = raw.trim();
            if (line.startsWith("----")) { inResults = true; continue; }
            if (!inResults || line.isBlank()) continue;

            String[] parts = line.split("\\s+");
            if (parts.length >= 2) {
                String name    = parts[0].trim();
                String version = parts[1].trim();
                packages.add(new Package(name, name, version, PackageSource.SCOOP));
            }
        }
        return packages;
    }

    private List<UpdateInfo> parseScoopStatus(String output) {
        List<UpdateInfo> updates = new ArrayList<>();
        if (output == null || output.isBlank()) return updates;

        boolean inResults = false;
        for (String raw : output.split("\n")) {
            String line = raw.trim();
            if (line.startsWith("----")) { inResults = true; continue; }
            if (!inResults || line.isBlank()) continue;

            
            String[] parts = line.split("\\s{2,}");
            if (parts.length >= 3) {
                String name    = parts[0].trim();
                String current = parts[1].trim();
                String newVer  = parts[2].trim();
                updates.add(new UpdateInfo(name, name, current, newVer, PackageSource.SCOOP));
            }
        }
        return updates;
    }
}

