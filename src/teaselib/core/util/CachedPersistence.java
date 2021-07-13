package teaselib.core.util;

import teaselib.core.Persistence;
import teaselib.core.configuration.PersistentConfigurationFile;

public interface CachedPersistence extends Persistence {
    PersistentConfigurationFile file();
}
