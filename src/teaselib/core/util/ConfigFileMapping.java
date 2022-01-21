package teaselib.core.util;

import java.util.Optional;

import teaselib.core.Persistence;
import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.ConfigurationFile;

/**
 * @author Citizen-Cane
 *
 */
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
        return config.getUserSettings(name.namespace);
    }

}
