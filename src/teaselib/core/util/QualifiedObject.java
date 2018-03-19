package teaselib.core.util;

public class QualifiedObject extends QualifiedItem<Object> {
    public QualifiedObject(Object value) {
        super(value);
        if (value instanceof Enum<?>) {
            throw new IllegalArgumentException(value.toString());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj instanceof QualifiedObject) {
            if (this == obj)
                return true;
            QualifiedObject other = (QualifiedObject) obj;
            if (value == null) {
                return other.value == null;
            } else {
                return value.toString().equalsIgnoreCase(other.value.toString());
            }
        } else if (obj instanceof Enum<?>) {
            return toString().equalsIgnoreCase(ReflectionUtils.qualifiedName((Enum<?>) obj));
        } else {
            return this.toString().equalsIgnoreCase(obj.toString());
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String namespace() {
        return namespaceOf(value);
    }

    @Override
    public String name() {
        return nameOf(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
