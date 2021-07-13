package teaselib.core.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import teaselib.core.util.SortedProperties;

public class ConfigurationFile {

    private final SortedProperties properties;

    public ConfigurationFile() {
        this.properties = new SortedProperties();
    }

    public ConfigurationFile(ConfigurationFile defaults) {
        this.properties = new SortedProperties(defaults.properties);
    }

    public void load(InputStream inputStream) throws IOException {
        properties.load(inputStream);
    }

    public boolean has(String key) {
        return properties.getProperty(key) != null;
    }

    public String get(String key) {
        return properties.getProperty(key);
    }

    public boolean getBoolean(String key) {
        return Boolean.toString(true).equalsIgnoreCase(get(key));
    }

    public void set(String key, String value) {
        if (value == null) {
            clear(key);
        } else {
            properties.setProperty(key, value);
        }
    }

    public void set(String key, boolean value) {
        set(key, value ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
    }

    public void clear(String key) {
        properties.remove(key);
    }

    public void store(OutputStream outputStream, String comments) throws IOException {
        properties.store(outputStream, comments);
    }

}
