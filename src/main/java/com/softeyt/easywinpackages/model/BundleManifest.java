package com.softeyt.easywinpackages.model;

import java.time.LocalDateTime;
import java.util.List;


public record BundleManifest(
        String manifestVersion,
        String name,
        LocalDateTime createdAt,
        List<BundleEntry> packages
) {
    
    public static final String CURRENT_VERSION = "1.0";
}

