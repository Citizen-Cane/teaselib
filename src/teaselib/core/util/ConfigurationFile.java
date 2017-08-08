package teaselib.core.util;

import java.io.File;
import java.util.Properties;

public class ConfigurationFile extends SortedProperties {
    private static final long serialVersionUID = 1L;

    final File file;

    public ConfigurationFile(File file) {
        super();
        this.file = file;
    }

    public ConfigurationFile(File file, Properties defaults) {
        super(defaults);
        this.file = file;
    }

}
