package teaselib.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import teaselib.core.util.ConfigurationFile;
import teaselib.core.util.FileUtilities;
import teaselib.core.util.QualifiedItem;
import teaselib.core.util.ReflectionUtils;

public class Configuration {
    private final List<Properties> defaults = new ArrayList<>();
    Properties persistentProperties;

    final Properties sessionProperties = new Properties();

    public static interface Setup {
        Configuration applyTo(Configuration config) throws IOException;
    }

    public Configuration() {
        persistentProperties = sessionProperties;
    }

    public Configuration(Setup setup) throws IOException {
        this();
        setup.applyTo(this);
    }

    public void addUserFile(Enum<?> setting, File preset, File userFile) throws IOException {
        set(setting, userFile.getAbsolutePath());
        addUserFile(preset, userFile);
    }

    public void addUserFile(File preset, File userFile) throws IOException {
        if (!userFile.exists()) {
            FileUtilities.copyFile(preset, userFile);
        }
    }

    public void addConfigFile(File file) throws IOException {
        ConfigurationFile configurationFile;
        if (defaults.isEmpty()) {
            configurationFile = new ConfigurationFile(file);
        } else {
            configurationFile = new ConfigurationFile(file, defaults.get(defaults.size() - 1));
        }
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            configurationFile.load(fileInputStream);
        }
        defaults.add(configurationFile);
        persistentProperties = configurationFile;
    }

    public boolean has(String property) {
        return has(QualifiedItem.of(property));
    }

    public boolean has(Enum<?> property) {
        return has(QualifiedItem.of(property));
    }

    public boolean has(QualifiedItem property) {
        String item = property.toString();
        return sessionProperties.containsKey(item) || System.getProperties().containsKey(item)
                || persistentProperties.containsKey(item);
    }

    public String get(String property) {
        return get(QualifiedItem.of(property));
    }

    public String get(Enum<?> property) {
        return get(QualifiedItem.of(property));
    }

    public String get(QualifiedItem property) {
        String item = property.toString();

        String value = sessionProperties.getProperty(item);
        if (value != null) {
            return value;
        }

        value = System.getProperty(item);
        if (value != null) {
            return value;
        }

        value = persistentProperties.getProperty(item);
        if (value != null) {
            return value;
        }

        throw new IllegalArgumentException("Property not found: " + item);
    }

    public Configuration set(String property, String value) {
        return set(QualifiedItem.of(property), value);
    }

    public Configuration set(Enum<?> property, String value) {
        return set(QualifiedItem.of(property), value);
    }

    public Configuration set(Enum<?> property, Enum<?> value) {
        return set(QualifiedItem.of(property), ReflectionUtils.qualifiedName(value));
    }

    public Configuration set(QualifiedItem property, String value) {
        sessionProperties.setProperty(property.toString(), value);
        return this;
    }

    public Configuration setSystemProperty(QualifiedItem property, String value) {
        System.getProperties().setProperty(property.toString(), value);
        return this;
    }
}
