package teaselib.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import teaselib.core.util.ConfigurationFile;
import teaselib.core.util.FileUtilities;
import teaselib.core.util.QualifiedItem;

public class Configuration {
    private final List<Properties> defaults = new ArrayList<Properties>();
    Properties properties;

    Properties temporal = new Properties();

    public Configuration() {
    }

    public void addUserFile(File preset, File userFile) throws IOException {
        if (!userFile.exists()) {
            FileUtilities.copyFile(preset, userFile);
        }
    }

    public void addConfigFile(File file) throws FileNotFoundException, IOException {
        ConfigurationFile configurationFile;
        if (defaults.isEmpty()) {
            configurationFile = new ConfigurationFile(file);
        } else {
            configurationFile = new ConfigurationFile(file, defaults.get(defaults.size() - 1));
        }
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            configurationFile.load(fileInputStream);
        } finally {
            fileInputStream.close();
        }
        defaults.add(configurationFile);
        properties = configurationFile;
    }

    public String get(QualifiedItem<?> property) {
        String item = property.toString();

        String value = temporal.getProperty(item);
        if (value != null) {
            return value;
        }

        value = System.getProperty(item);
        if (value != null) {
            return value;
        }

        value = properties.getProperty(item);
        if (value != null) {
            return value;
        }

        throw new IllegalArgumentException("Property not found:" + item);
    }

    public void set(QualifiedItem<?> property, String value) {
        temporal.setProperty(property.toString(), value);
    }
}
