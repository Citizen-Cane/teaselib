package teaselib.core.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import teaselib.core.util.SortedProperties;

public class ConfigurationFile extends SortedProperties {
    private static final long serialVersionUID = 1L;

    public ConfigurationFile() {
        super();
    }

    public ConfigurationFile(Properties defaults) {
        super(defaults);
    }

    // TODO Move loading to super class

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

}
