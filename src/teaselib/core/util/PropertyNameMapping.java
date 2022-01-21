package teaselib.core.util;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Citizen-Cane
 *
 */
public abstract class PropertyNameMapping {
    public static final PropertyNameMapping NONE = new PropertyNameMapping() {

        @Override
        protected QualifiedName mapDomainsAndPaths(QualifiedName name) {
            return name;
        }

        @Override
        protected QualifiedName mapNames(QualifiedName name) {
            return name;
        }

        @Override
        protected String mapValueFromHost(QualifiedName name, String value) {
            return value;
        }

        @Override
        protected String mapValueToHost(QualifiedName name, String value) {
            return value;
        }

    };

    public QualifiedName map(QualifiedName name) {
        return mapDomainsAndPaths(mapNames(name));
    }

    protected abstract QualifiedName mapDomainsAndPaths(QualifiedName name);

    protected abstract QualifiedName mapNames(QualifiedName name);

    protected abstract String mapValueFromHost(QualifiedName name, String value);

    protected abstract String mapValueToHost(QualifiedName name, String value);

    public final String get(QualifiedName name, Supplier<String> value) {
        return mapValueFromHost(map(name), value.get());
    }

    // TODO Use void function with name, value parameter pair instead of consumer
    public final void set(QualifiedName name, String value, Consumer<String> persistence) {
        persistence.accept(mapValueToHost(name, value));
    }

}
