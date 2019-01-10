package teaselib.core.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import teaselib.core.util.FileUtilities;
import teaselib.core.util.QualifiedItem;
import teaselib.core.util.ReflectionUtils;

/**
 * Builds a chain of property files to store a system configuration.
 * <li>default settings
 * <li>user configuration
 * <li>system properties (as -D option)
 * <li>non-persistent session properties (changed at runtime)
 * 
 * @author Citizen-Cane
 *
 */
public class Configuration {
    static final String DEFAULTS = ReflectionUtils.absolutePath(Configuration.class) + "defaults/";

    private final Map<String, ConfigurationFile> userPropertiesNamespaceMapping = new HashMap<>();
    final PersistentConfigurationFiles persistentConfigurationFiles = new PersistentConfigurationFiles();

    private final List<Properties> defaultProperties = new ArrayList<>();
    private final Properties sessionProperties = new Properties();
    private Properties persistentProperties;

    Optional<File> userPath = Optional.empty();

    public Configuration() {
        this.persistentProperties = sessionProperties;
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

    public void addDefaultProperties(String defaults, String properties, String... namespaces) throws IOException {
        addDefaultUserProperties(Optional.of(defaults), properties, Arrays.asList(namespaces));
    }

    public void addDefaultProperties(String defaults, String properties, List<String> namespaces) throws IOException {
        addDefaultUserProperties(Optional.of(defaults), properties, namespaces);
    }

    public void addPersistentUserProperties(String defaults, String properties, File userPath, String... namespaces)
            throws IOException {
        addPersistentUserProperties(defaults, properties, userPath, Arrays.asList(namespaces));
    }

    public void addPersistentUserProperties(String defaults, String properties, File userPath, List<String> namespaces)
            throws IOException {
        addPersistentUserProperties(Optional.of(defaults), properties, userPath, namespaces);
    }

    public void addScriptSettings(String properties, String namespace) throws IOException {
        if (userPath.isPresent()) {
            addPersistentUserProperties(Optional.empty(), properties, userPath.get(), Arrays.asList(namespace));
        } else {
            addDefaultUserProperties(Optional.empty(), properties, Arrays.asList(namespace));
        }
    }

    private void addDefaultUserProperties(Optional<String> defaults, String properties, List<String> namespaces)
            throws IOException {
        if (namespaceAlreadyRegistered(namespaces)) {
            throw new IllegalArgumentException("Namespace already registered: " + namespaces);
        }

        ConfigurationFile p = new ConfigurationFile();
        if (defaults.isPresent()) {
            try (InputStream stream = getClass().getResourceAsStream(defaults.get() + properties)) {
                p.load(stream);
            }
        }

        for (String string : namespaces) {
            userPropertiesNamespaceMapping.put(string, p);
        }
    }

    void addPersistentUserProperties(Optional<String> defaultResource, String properties, File persistentPath,
            List<String> namespaces) throws IOException {
        if (namespaceAlreadyRegistered(namespaces)) {
            throw new IllegalArgumentException("Namespace already registered: " + namespaces);
        }

        File userFile = new File(persistentPath, properties);
        if (defaultResource.isPresent()) {
            createUserFileIfNotExisits(defaultResource + properties, userFile);
        }
        ConfigurationFile p = persistentConfigurationFiles
                .newFile(Paths.get(persistentPath.getAbsolutePath(), properties));

        for (String string : namespaces) {
            userPropertiesNamespaceMapping.put(string, p);
        }
    }

    private boolean namespaceAlreadyRegistered(List<String> namespaces) {
        return namespaces.stream().anyMatch(userPropertiesNamespaceMapping::containsKey);
    }

    public Optional<ConfigurationFile> getUserSettings(String namespace) {
        return Optional.ofNullable(userPropertiesNamespaceMapping.get(namespace));
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
        if (defaultProperties.isEmpty()) {
            configurationFile = new ConfigurationFile();
        } else {
            configurationFile = new ConfigurationFile(defaultProperties.get(defaultProperties.size() - 1));
        }
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            configurationFile.load(fileInputStream);
        }
        defaultProperties.add(configurationFile);
        persistentProperties = configurationFile;
    }

    public void add(String configResource) throws IOException {
        ConfigurationFile configurationFile;
        if (defaultProperties.isEmpty()) {
            configurationFile = new ConfigurationFile();
        } else {
            configurationFile = new ConfigurationFile(defaultProperties.get(defaultProperties.size() - 1));
        }
        try (InputStream fileInputStream = getClass().getResourceAsStream(configResource)) {
            Objects.requireNonNull(fileInputStream, "Configuration file not found:" + configResource);
            configurationFile.load(fileInputStream);
        }
        defaultProperties.add(configurationFile);
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
