package teaselib.core.util;

import teaselib.core.Persistence;
import teaselib.core.configuration.ConfigurationFile;

public interface CachedPersistence extends Persistence {
    ConfigurationFile file();
}
