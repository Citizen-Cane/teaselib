package teaselib.core.util;

public class QualifiedEnum extends AbstractQualifiedItem<Enum<?>> {

    public QualifiedEnum(Enum<?> value) {
        super(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj instanceof QualifiedEnum) {
            if (this == obj)
                return true;
            QualifiedEnum other = (QualifiedEnum) obj;
            if (value == null) {
                return other.value == null;
            } else {
                return value.equals(other.value);
            }
        } else if (obj instanceof Enum<?>) {
            return value == obj;
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
        return ReflectionUtils.qualified(value.getClass());
    }

    @Override
    public String name() {
        return value.name();
    }

    @Override
    public String toString() {
        return ReflectionUtils.qualifiedName(value);
    }
}
