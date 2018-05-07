package teaselib.core.util;

abstract class AbstractQualifiedItem<T> extends QualifiedItem {
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
    public abstract boolean equals(Object obj);

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + toString().toLowerCase().hashCode();
        return result;
    }

}
