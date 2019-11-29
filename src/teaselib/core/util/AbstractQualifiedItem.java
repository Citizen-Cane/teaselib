package teaselib.core.util;

import java.util.Optional;

abstract class AbstractQualifiedItem<T> implements QualifiedItem {
    public final T value;

    public AbstractQualifiedItem(T value) {
        if (value == null) {
            throw new IllegalArgumentException("null");
        }
        this.value = value;
    }

    @Override
    public boolean is(Object obj) {
        return equals(obj);
    }

    @Override
    public T value() {
        return value;
    }

    @Override
    public Optional<String> guid() {
        return Optional.empty();
    }

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + toString().toLowerCase().hashCode();
        return result;
    }

    String toString(String path, Optional<String> guid) {
        if (guid.isPresent()) {
            return path + "#" + guid.get();
        } else {
            return path;
        }
    }

}
