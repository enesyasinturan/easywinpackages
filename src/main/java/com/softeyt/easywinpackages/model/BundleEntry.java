package com.softeyt.easywinpackages.model;


public record BundleEntry(
        String id,
        String name,
        PackageSource source,
        String version
) {}

