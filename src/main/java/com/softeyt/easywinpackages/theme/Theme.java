package com.softeyt.easywinpackages.theme;


public enum Theme {
    SYSTEM,
    LIGHT,
    DARK;

    
    public static Theme fromStoredValue(String value) {
        if (value == null || value.isBlank()) {
            return SYSTEM;
        }
        try {
            return Theme.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return SYSTEM;
        }
    }
}

