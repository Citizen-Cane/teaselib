package teaselib.core.util;

import teaselib.core.Persistence;

/**
 * @author Citizen-Cane
 *
 */
public class PropertyNameMappingPersistence implements Persistence {
    private final Persistence persistence;
    private final PropertyNameMapping mapping;

    public PropertyNameMappingPersistence(Persistence persistence, PropertyNameMapping mapping) {
        this.persistence = persistence;
        this.mapping = mapping;
    }

    @Override
    public boolean has(QualifiedName name) {
        return persistence.has(mapping.map(name));
    }

    @Override
    public String get(QualifiedName name) {
        return mapping.get(name, () -> persistence.get(mapping.map(name)));
    }

    @Override
    public void set(QualifiedName name, String value) {
        if (value == null) {
            clear(mapping.map(name));
        } else {
            mapping.set(name, value, v -> persistence.set(mapping.map(name), v));
        }
    }

    @Override
    public boolean getBoolean(QualifiedName name) {
        return persistence.getBoolean(mapping.map(name));
    }

    @Override
    public void set(QualifiedName name, boolean value) {
        persistence.set(mapping.map(name), value);
    }

    @Override
    public void clear(QualifiedName name) {
        persistence.clear(mapping.map(name));
    }

}
