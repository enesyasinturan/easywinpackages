package com.softeyt.easywinpackages.model;


public record UpdateInfo(
        String id,
        String name,
        String currentVersion,
        String newVersion,
        PackageSource source
) {}

