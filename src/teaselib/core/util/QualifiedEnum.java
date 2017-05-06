package teaselib.core.util;

public class QualifiedEnum extends QualifiedItem<Enum<?>> {

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
            return this.toString().equals(obj.toString());
        }
    }

    @Override
    public String namespace() {
        return ReflectionUtils.normalizeClassName(value.getClass());
    }

    @Override
    public String name() {
        return value.name();
    }

    @Override
    public String toString() {
        return toString(value);
    }

    public static String toString(Enum<?> value) {
        return ReflectionUtils.normalizeClassName(value.getClass()) + "." + value.name();
    }
}
