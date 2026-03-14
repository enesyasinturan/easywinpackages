package com.softeyt.easywinpackages.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.softeyt.easywinpackages.model.BundleEntry;
import com.softeyt.easywinpackages.model.BundleManifest;
import com.softeyt.easywinpackages.model.Package;
import com.softeyt.easywinpackages.model.PackageSource;
import com.softeyt.easywinpackages.service.impl.ChocoService;
import com.softeyt.easywinpackages.service.impl.ScoopService;
import com.softeyt.easywinpackages.service.impl.WingetService;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class BundleService {

    private static final Logger log = LoggerFactory.getLogger(BundleService.class);

    private final ObjectMapper mapper;
    private final WingetService wingetService;
    private final ChocoService chocoService;
    private final ScoopService scoopService;

    @Inject
    public BundleService(ObjectMapper mapper,
                         WingetService wingetService,
                         ChocoService chocoService,
                         ScoopService scoopService) {
        this.mapper = mapper;
        this.wingetService = wingetService;
        this.chocoService = chocoService;
        this.scoopService = scoopService;
    }

    public void export(List<BundleEntry> entries, String bundleName, Path outputPath) throws IOException {
        BundleManifest manifest = new BundleManifest(
                BundleManifest.CURRENT_VERSION,
                bundleName,
                LocalDateTime.now(),
                List.copyOf(entries));
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(outputPath.toFile(), manifest);
    }

    public BundleManifest importBundle(Path bundlePath) throws IOException {
        return mapper.readValue(bundlePath.toFile(), BundleManifest.class);
    }

    public List<String> validateSources(BundleManifest manifest) {
        List<String> missing = new ArrayList<>();
        boolean needsWinget = manifest.packages().stream().anyMatch(e -> e.source() == PackageSource.WINGET);
        boolean needsChoco = manifest.packages().stream().anyMatch(e -> e.source() == PackageSource.CHOCO);
        boolean needsScoop = manifest.packages().stream().anyMatch(e -> e.source() == PackageSource.SCOOP);

        if (needsWinget && !wingetService.isAvailable()) {
            missing.add(wingetService.getDisplayName());
        }
        if (needsChoco && !chocoService.isAvailable()) {
            missing.add(chocoService.getDisplayName());
        }
        if (needsScoop && !scoopService.isAvailable()) {
            missing.add(scoopService.getDisplayName());
        }
        return missing;
    }

    public Task<Void> installAll(BundleManifest manifest) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<BundleEntry> entries = manifest.packages();
                int total = entries.size();
                for (int i = 0; i < total; i++) {
                    BundleEntry entry = entries.get(i);
                    updateMessage("Installing " + entry.name() + "...");
                    updateProgress(i, total);

                    PackageManagerService service = resolveService(entry.source());
                    if (service == null || !service.isAvailable()) {
                        log.warn("Skipping {} - {} not available", entry.id(), entry.source());
                        continue;
                    }

                    Package pkg = new Package(entry.id(), entry.name(), entry.version(), entry.source());
                    Task<Void> installTask = service.install(pkg);
                    installTask.run();
                    installTask.get();
                    log.info("Installed {}/{}: {}", i + 1, total, entry.id());
                }
                updateProgress(total, total);
                return null;
            }
        };
    }

    private PackageManagerService resolveService(PackageSource source) {
        return switch (source) {
            case WINGET -> wingetService;
            case CHOCO -> chocoService;
            case SCOOP -> scoopService;
        };
    }
}

