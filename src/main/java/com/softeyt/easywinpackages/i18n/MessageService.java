package com.softeyt.easywinpackages.i18n;

import com.google.inject.Singleton;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;


@Singleton
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);
    private static final String BUNDLE_BASE = "com/softeyt/easywinpackages/i18n/messages";

    private final ObjectProperty<Locale> localeProperty =
            new SimpleObjectProperty<>(Locale.ENGLISH);

    private ResourceBundle bundle;

    public MessageService() {
        loadBundle(Locale.ENGLISH);
    }

    
    public void changeLocale(Locale locale) {
        if (locale == null) throw new IllegalArgumentException("locale must not be null");
        loadBundle(locale);
        localeProperty.set(locale);
        log.info("Locale changed to {}", locale);
    }

    
    public StringBinding bind(String key) {
        return new StringBinding() {
            { super.bind(localeProperty); }

            @Override
            protected String computeValue() {
                return getString(key);
            }
        };
    }

    
    public StringBinding bind(String key, Object... params) {
        return new StringBinding() {
            { super.bind(localeProperty); }

            @Override
            protected String computeValue() {
                return getString(key, params);
            }
        };
    }

    
    public String getString(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            log.warn("Missing i18n key: {}", key);
            return "!" + key + "!";
        }
    }

    
    public String getString(String key, Object... params) {
        return MessageFormat.format(getString(key), params);
    }

    
    public Locale getLocale() {
        return localeProperty.get();
    }

    
    public ObjectProperty<Locale> localeProperty() {
        return localeProperty;
    }

    private void loadBundle(Locale locale) {
        bundle = ResourceBundle.getBundle(BUNDLE_BASE, locale,
                Thread.currentThread().getContextClassLoader());
    }
}

