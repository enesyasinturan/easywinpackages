package com.softeyt.easywinpackages.model;

public record Package(
        String id,
        String name,
        String version,
        PackageSource source
) {}