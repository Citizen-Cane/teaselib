package teaselib.core.configuration;

import static java.util.Collections.singletonList;
import static teaselib.core.util.ExceptionUtil.asRuntimeException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import teaselib.core.Closeable;
import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.FileUtilities;
import teaselib.core.util.QualifiedItem;
import teaselib.core.util.QualifiedName;
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
public class Configuration implements Closeable {
    static final String DEFAULTS = ReflectionUtils.absolutePath(Configuration.class) + "defaults/";

    static final String SCRIPT_SETTINGS = "Script Settings";
    static final String PROPERTIES_EXTENSION = ".properties";

    private final Map<String, ConfigurationFile> userPropertiesNamespaceMapping = new HashMap<>();
    final PersistentConfigurationFileStoreService persistentConfigurationFiles = new PersistentConfigurationFileStoreService();

    private final List<ConfigurationFile> defaultProperties = new ArrayList<>();
    private final ConfigurationFile sessionProperties = new ConfigurationFile();
    private ConfigurationFile persistentProperties;

    private Optional<File> scriptSettingsFolder = Optional.empty();

    public Configuration() {
        this.persistentProperties = sessionProperties;
    }

    public Configuration(Setup setup) throws IOException {
        this();
        setup.applyTo(this);
    }

    public PersistentConfigurationFile addPersistentConfigurationFile(Path path) throws IOException {
        return persistentConfigurationFiles.openFile(path);
    }

    public void add(String defaults, String properties, File userPath) throws IOException {
        add(defaults + properties);
        var userConfig = new File(userPath, properties);
        initUserFileWithDefaults(defaults + properties, userConfig);
        add(userConfig);
    }

    public void addDefaultProperties(String defaults, String resource, List<String> namespaces) throws IOException {
        addDefaultUserProperties(Optional.of(defaults), resource, namespaces);
    }

    void addPersistentUserProperties(String defaults, String file, File folder, List<String> namespaces)
            throws IOException {
        addPersistentUserProperties(Optional.of(defaults), file, folder, QualifiedName.strip(namespaces));
    }

    void addPersistentUserProperties(String file, File folder, String namespace) throws IOException {
        addPersistentUserProperties(Optional.empty(), file, folder, Collections.singletonList(namespace));
    }

    public void addScriptSettings(String namespace) throws IOException {
        String filename = QualifiedName.strip(namespace) + PROPERTIES_EXTENSION;
        List<String> namespaces = Collections.singletonList(QualifiedName.strip(namespace));
        if (scriptSettingsFolder.isPresent()) {
            addPersistentUserProperties(Optional.empty(), filename, scriptSettingsFolder.get(), namespaces);
        } else {
            addDefaultUserProperties(Optional.empty(), filename, namespaces);
        }
    }

    private void addDefaultUserProperties(Optional<String> defaults, String resource, List<String> namespaces)
            throws IOException {
        if (namespaceAlreadyRegistered(namespaces)) {
            throw new IllegalArgumentException("Namespace already registered: " + namespaces);
        } else {
            var configurationFile = new ConfigurationFile();
            if (defaults.isPresent()) {
                try (InputStream stream = getClass().getResourceAsStream(defaults.get() + resource)) {
                    configurationFile.load(stream);
                }
            }
            registerNamespaces(namespaces, configurationFile);
        }
    }

    private void addPersistentUserProperties(Optional<String> defaultResource, String filename, File folder,
            List<String> namespaces) throws IOException {
        if (namespaceAlreadyRegistered(namespaces)) {
            if (namespaces.size() != 1) {
                throw new IllegalArgumentException(namespaces.toString());
            } else {
                String existing = namespaces.get(0) + PROPERTIES_EXTENSION;
                if (!filename.equalsIgnoreCase(existing)) {
                    throw new IllegalArgumentException(filename);
                }
            }
        } else {
            var userFile = new File(folder, filename);
            if (defaultResource.isPresent()) {
                initUserFileWithDefaults(defaultResource.get() + filename, userFile);
            }
            ConfigurationFile configurationFile = persistentConfigurationFiles
                    .openFile(Paths.get(folder.getAbsolutePath(), filename));
            registerNamespaces(namespaces, configurationFile);
        }

    }

    private boolean namespaceAlreadyRegistered(List<String> namespaces) {
        return namespaces.stream().anyMatch(userPropertiesNamespaceMapping::containsKey);
    }

    private void registerNamespaces(List<String> namespaces, ConfigurationFile p) {
        for (String namespace : namespaces) {
            userPropertiesNamespaceMapping.put(namespace, p);
        }
    }

    public Optional<ConfigurationFile> getUserSettings(String namespace) {
        return userPropertiesNamespaceMapping.entrySet().stream().filter(e -> namespace.startsWith(e.getKey()))
                .map(Entry<String, ConfigurationFile>::getValue).findFirst();
    }

    public void addUserFile(Enum<?> setting, String templateResource, File userFile) throws IOException {
        set(setting, userFile.getAbsolutePath());
        initUserFileWithDefaults(templateResource, userFile);
    }

    public void initUserFileWithDefaults(String templateResource, File userFile) throws IOException {
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
        try (var fileInputStream = new FileInputStream(file)) {
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
        try (var fileInputStream = getClass().getResourceAsStream(configResource)) {
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
        var item = property.toString();

        return sessionProperties.has(item) || //
                System.getProperties().containsKey(item) || //
                persistentProperties.has(item);
    }

    public String get(String property) {
        return get(QualifiedItem.of(property));
    }

    public String get(Enum<?> property) {
        return get(QualifiedItem.of(property));
    }

    public String get(QualifiedItem property) {
        var item = property.toString();

        String value = sessionProperties.get(item);
        if (value != null) {
            return value;
        }

        value = System.getProperty(item);
        if (value != null) {
            return value;
        }

        value = persistentProperties.get(item);
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
        sessionProperties.set(property.toString(), value);
        return this;
    }

    public Configuration setSystemProperty(QualifiedItem property, String value) {
        System.getProperties().setProperty(property.toString(), value);
        return this;
    }

    public void setUserPath(Optional<File> path) {
        if (path.isPresent()) {
            var settings = new File(path.get(), SCRIPT_SETTINGS);
            settings.mkdirs();
            File[] files = settings.listFiles(Configuration::settingsFile);
            Arrays.stream(files).map(File::getName).forEach(name -> {
                int index = name.lastIndexOf('.');
                if (index > 0) {
                    var namespace = name.substring(0, index);
                    try {
                        addPersistentUserProperties(Optional.empty(), namespace + PROPERTIES_EXTENSION, settings,
                                singletonList(namespace));
                    } catch (IOException e) {
                        throw asRuntimeException(e);
                    }
                }
            });
            scriptSettingsFolder = Optional.of(settings);
        } else {
            scriptSettingsFolder = Optional.empty();
        }
    }

    public static boolean settingsFile(File file) {
        return file.getName().endsWith(PROPERTIES_EXTENSION);
    }

    public void flushSettings() {
        try {
            persistentConfigurationFiles.write();
        } catch (IOException e) {
            throw asRuntimeException(ExceptionUtil.reduce(e));
        }
    }

    @Override
    public void close() {
        persistentConfigurationFiles.close();
    }

}
