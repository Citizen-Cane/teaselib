package teaselib.core.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import teaselib.core.util.FileUtilities;
import teaselib.core.util.QualifiedItem;
import teaselib.core.util.ReflectionUtils;
import teaselib.core.util.SortedProperties;

public class Configuration {
    static final String DEFAULTS = ReflectionUtils.absolutePath(Configuration.class) + "defaults/";

    private final List<Properties> defaults = new ArrayList<>();
    Properties persistentProperties;

    final Properties sessionProperties = new Properties();

    public Configuration() {
        persistentProperties = sessionProperties;
    }

    public Configuration(Setup setup) throws IOException {
        this();
        setup.applyTo(this);
    }

    public void add(String defaults, String properties, File userPath) throws IOException {
        add(defaults + properties);
        File userConfig = new File(userPath, properties);
        createUserFileIfNotExisits(defaults + properties, userConfig);
        add(userConfig);
    }

    private final Map<String, Properties> userPropertiesNamespaceMapping = new HashMap<>();

    public void addDefaultProperties(String defaults, String properties, String... namespaces) throws IOException {
        addUserProperties(defaults, properties, null, Arrays.asList(namespaces));
    }

    public void addUserProperties(String defaults, String properties, File userPath, String... namespaces)
            throws IOException {
        addUserProperties(defaults, properties, userPath, Arrays.asList(namespaces));
    }

    public void addUserProperties(String defaults, String properties, File userPath, List<String> namespaces)
            throws IOException {
        SortedProperties p = new SortedProperties();

        if (userPath != null) {
            File file = new File(userPath, properties);
            createUserFileIfNotExisits(defaults + properties, file);
            try (InputStream stream = new FileInputStream(file)) {
                Objects.requireNonNull(stream, "User properties file not found:" + properties);
                p.load(stream);
                // TODO Save to disk on write
            }
        } else {
            try (InputStream stream = getClass().getResourceAsStream(defaults + properties)) {
                p.load(stream);
            }
        }

        for (String string : namespaces) {
            userPropertiesNamespaceMapping.put(string, p);
        }
    }

    public Properties getProperties(String namespace) {
        throw new UnsupportedOperationException("TODO implement!");
    }

    public void addUserFile(Enum<?> setting, String templateResource, File userFile) throws IOException {
        set(setting, userFile.getAbsolutePath());
        createUserFileIfNotExisits(templateResource, userFile);
    }

    public void createUserFileIfNotExisits(String templateResource, File userFile) throws IOException {
        if (!userFile.exists()) {
            FileUtilities.copy(templateResource, userFile);
        }
    }

    public void add(File file) throws IOException {
        ConfigurationFile configurationFile;
        if (defaults.isEmpty()) {
            configurationFile = new ConfigurationFile();
        } else {
            configurationFile = new ConfigurationFile(defaults.get(defaults.size() - 1));
        }
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            configurationFile.load(fileInputStream);
        }
        defaults.add(configurationFile);
        persistentProperties = configurationFile;
    }

    public void add(String configResource) throws IOException {
        ConfigurationFile configurationFile;
        if (defaults.isEmpty()) {
            configurationFile = new ConfigurationFile();
        } else {
            configurationFile = new ConfigurationFile(defaults.get(defaults.size() - 1));
        }
        try (InputStream fileInputStream = getClass().getResourceAsStream(configResource)) {
            Objects.requireNonNull(fileInputStream, "Configuration file not found:" + configResource);
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
