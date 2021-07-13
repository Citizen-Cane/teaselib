package teaselib.core.util;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

import teaselib.Actor;
import teaselib.Sexuality.Gender;
import teaselib.core.Persistence;
import teaselib.core.TeaseLib;
import teaselib.core.UserItems;
import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.ConfigurationFile;
import teaselib.util.TextVariables;

public class ConfigFileMapping implements Persistence {

    private final Configuration config;
    private final Persistence persistence;

    public ConfigFileMapping(Configuration config, Persistence persistence) {
        this.config = config;
        this.persistence = persistence;
    }

    @Override
    public boolean has(QualifiedName name) {
        Optional<ConfigurationFile> properties = getConfigFile(name);
        if (properties.isPresent() && properties.get().has(name.toString())) {
            return true;
        } else {
            return persistence.has(name);
        }
    }

    @Override
    public String get(QualifiedName name) {
        Optional<ConfigurationFile> properties = getConfigFile(name);
        if (properties.isPresent()) {
            return properties.get().get(name.toString());
        } else {
            return persistence.get(name);
        }
    }

    @Override
    public boolean getBoolean(QualifiedName name) {
        Optional<ConfigurationFile> properties = getConfigFile(name);
        if (properties.isPresent()) {
            return properties.get().getBoolean(name.toString());
        } else {
            return persistence.getBoolean(name);
        }
    }

    @Override
    public void set(QualifiedName name, String value) {
        Optional<ConfigurationFile> properties = getConfigFile(name);
        if (properties.isPresent()) {
            properties.get().set(name.toString(), value);
            if (persistence.has(name)) {
                persistence.set(name, value);
            }
        } else {
            persistence.set(name, value);
        }
    }

    @Override
    public void set(QualifiedName name, boolean value) {
        Optional<ConfigurationFile> properties = getConfigFile(name);
        if (properties.isPresent()) {
            properties.get().set(name.toString(), value);
            if (persistence.has(name)) {
                persistence.set(name, value);
            }
        } else {
            persistence.set(name, value);
        }
    }

    @Override
    public void clear(QualifiedName name) {
        Optional<ConfigurationFile> properties = getConfigFile(name);
        if (properties.isPresent()) {
            properties.get().clear(name.toString());
        }
        persistence.clear(name);
    }

    private Optional<ConfigurationFile> getConfigFile(QualifiedName name) {
        Optional<ConfigurationFile> properties = config.getUserSettings(name.namespace);
        if (properties.isEmpty()) {
            properties = config.getUserSettings(name.domain);
        }
        return properties;
    }

    @Override
    public UserItems getUserItems(TeaseLib teaseLib) throws IOException {
        return persistence.getUserItems(teaseLib);
    }

    @Override
    public TextVariables getTextVariables(Locale locale) {
        return persistence.getTextVariables(locale);
    }

    @Override
    public Actor getDominant(Gender gender, Locale locale) {
        return persistence.getDominant(gender, locale);
    }

}
