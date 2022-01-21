package teaselib.core.debug;

import teaselib.core.Persistence;
import teaselib.core.util.QualifiedName;

public class DebugPersistence implements Persistence {
    public static final String TRUE = "true";
    public static final String FALSE = "false";

    public final DebugStorage storage;

    public DebugPersistence(DebugStorage storage) {
        this.storage = storage;
    }

    @Override
    public boolean has(QualifiedName name) {
        return storage.containsKey(name);
    }

    @Override
    public String get(QualifiedName name) {
        return storage.get(name);
    }

    @Override
    public void set(QualifiedName name, String value) {
        if (value == null) {
            clear(name);
        } else {
            storage.put(name, value);
        }
    }

    @Override
    public boolean getBoolean(QualifiedName name) {
        String value = get(name);
        if (value == null) {
            return false;
        } else {
            return value.equals(TRUE);
        }
    }

    @Override
    public void set(QualifiedName name, boolean value) {
        set(name, value ? TRUE : FALSE);
    }

    @Override
    public void clear(QualifiedName name) {
        storage.remove(name);
    }

    @Override
    public String toString() {
        return storage.toString();
    }
}
