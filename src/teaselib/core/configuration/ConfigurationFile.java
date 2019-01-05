package teaselib.core.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import teaselib.core.util.SortedProperties;

public class ConfigurationFile extends SortedProperties {
    private static final long serialVersionUID = 1L;

    private AtomicBoolean writeBack = new AtomicBoolean(false);

    public ConfigurationFile() {
        super();
    }

    public ConfigurationFile(Properties defaults) {
        super(defaults);
    }

    // TODO Move loading resource to super class

    public ConfigurationFile(String resource) throws IOException {
        super();
        try (InputStream data = getClass().getResourceAsStream(resource)) {
            load(data);
        }
    }

    public ConfigurationFile(InputStream data) throws IOException {
        super();
        load(data);
    }

    public boolean has(String key) {
        return containsKey(key);
    }

    public String get(String key) {
        return getProperty(key);
    }

    public boolean getBoolean(String key) {
        return Boolean.toString(true).equalsIgnoreCase(getProperty(key));
    }

    public void set(String key, String value) {
        setProperty(key, value);
        writeBack.set(true);
    }

    public void set(String key, boolean value) {
        set(key, value ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
    }

    public void clear(String key) {
        remove(key);
        writeBack.set(true);
    }

}
