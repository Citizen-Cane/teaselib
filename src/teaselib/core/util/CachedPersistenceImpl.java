package teaselib.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.configuration.ConfigurationFile;

/**
 * @author Citizen-Cane
 *
 */
public class CachedPersistenceImpl implements CachedPersistence {

    private static final Logger logger = LoggerFactory.getLogger(CachedPersistenceImpl.class);

    private final ConfigurationFile file;

    public CachedPersistenceImpl(ConfigurationFile file) {
        this.file = file;
    }

    @Override
    public ConfigurationFile file() {
        return file;
    }

    @Override
    public boolean has(QualifiedName name) {
        return has(name.toString());
    }

    private boolean has(String name) {
        return file.has(name);
    }

    @Override
    public String get(QualifiedName name) {
        return get(name.toString());
    }

    private String get(String name) {
        return file.get(name);
    }

    @Override
    public void set(QualifiedName name, String value) {
        set(name.toString(), value);

    }

    private void set(String name, String value) {
        file.set(name, value);
    }

    @Override
    public boolean getBoolean(QualifiedName name) {
        return getBoolean(name.toString());
    }

    private boolean getBoolean(String name) {
        return file.getBoolean(name);
    }

    @Override
    public void set(QualifiedName name, boolean value) {
        set(name.toString(), value);
    }

    private void set(String name, boolean value) {
        file.set(name, value);
    }

    @Override
    public void clear(QualifiedName name) {
        clear(name.toString());
    }

    private void clear(String name) {
        file.clear(name);
    }

}
