package com.softeyt.easywinpackages.model;


public record AppSettings(
        String theme,
        String locale
) {
    
    public static AppSettings defaults() {
        return new AppSettings("SYSTEM", "en");
    }
}

