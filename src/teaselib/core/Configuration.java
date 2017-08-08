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

    public String get(QualifiedItem<?> setting) {
        String name = setting.toString();

        String systemProperty = System.getProperty(name);
        if (name != null) {
            return systemProperty;
        }

        return properties.getProperty(name);
    }

    public void put(QualifiedItem<?> setting, String value) {
        properties.setProperty(setting.toString(), value);
        throw new UnsupportedOperationException("Implement persistence");
    }

}
