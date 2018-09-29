package teaselib.core.util;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import teaselib.core.util.Persist.Factory;

public class Storage {
    private final Iterator<String> field;
    private final Optional<Factory> factory;

    Storage(List<String> fields) {
        this.field = fields.iterator();
        this.factory = Optional.empty();
    }

    Storage(List<String> fields, Factory factory) {
        this.field = fields.iterator();
        this.factory = Optional.of(factory);
    }

    public <T> T next() {
        return Persist.from(field.next());
    }

    public boolean hasNext() {
        return field.hasNext();
    }

    @SuppressWarnings("unchecked")
    public <T> T getInstance(Class<T> clazz) {
        if (factory.isPresent()) {
            return (T) factory.get().get(clazz);
        } else {
            throw new IllegalArgumentException("Provide class factory for " + clazz.getName());
        }
    }

    public static Storage from(String persisted) {
        return new PersistedObject(persisted).toStorage();
    }
}