package teaselib.core.util;

public abstract class QualifiedItem<T> {
    public final T value;

    public QualifiedItem(T value) {
        if (value == null) {
            throw new IllegalArgumentException("null");
        }
        this.value = value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

}
