package teaselib.core.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import teaselib.core.util.SortedProperties;

public class ConfigurationFileImpl implements ConfigurationFile {

    private final Properties properties;

    public ConfigurationFileImpl() {
        this(new SortedProperties());
    }

    public ConfigurationFileImpl(ConfigurationFileImpl defaults) {
        this(new SortedProperties(defaults.properties));
    }

    private ConfigurationFileImpl(Properties properties) {
        this.properties = properties;
    }

    public void load(InputStream inputStream) throws IOException {
        properties.load(inputStream);
    }

    @Override
    public boolean has(String key) {
        return properties.getProperty(key) != null;
    }

    @Override
    public String get(String key) {
        return properties.getProperty(key);
    }

    @Override
    public final boolean getBoolean(String key) {
        return Boolean.toString(true).equalsIgnoreCase(get(key));
    }

    @Override
    public void set(String key, String value) {
        if (value == null) {
            clear(key);
        } else {
            properties.setProperty(key, value);
        }
    }

    @Override
    public final void set(String key, boolean value) {
        set(key, value ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
    }

    @Override
    public void clear(String key) {
        properties.remove(key);
    }

    public void store(OutputStream outputStream, String comments) throws IOException {
        properties.store(outputStream, comments);
    }

}
