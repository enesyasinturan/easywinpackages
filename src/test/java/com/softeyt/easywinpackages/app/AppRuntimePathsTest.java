package com.softeyt.easywinpackages.app;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppRuntimePathsTest {

    @Test
    void resolveApplicationBaseDirectory_usesProjectRootWhenRunningFromTargetClasses() {
        Path codeSourcePath = Path.of("C:/Users/test/easywinpackages/target/classes");

        Path resolvedBaseDirectory = AppRuntimePaths.resolveApplicationBaseDirectory(codeSourcePath);

        assertEquals(Path.of("C:/Users/test/easywinpackages"), resolvedBaseDirectory);
    }

    @Test
    void resolveApplicationBaseDirectory_usesParentDirectoryWhenRunningFromPackagedArtifact() {
        Path codeSourcePath = Path.of("C:/Program Files/EasyWinPackages/easywinpackages-fat.jar");

        Path resolvedBaseDirectory = AppRuntimePaths.resolveApplicationBaseDirectory(codeSourcePath);

        assertEquals(Path.of("C:/Program Files/EasyWinPackages"), resolvedBaseDirectory);
    }
}

