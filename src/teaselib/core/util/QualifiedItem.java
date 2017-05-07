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

    public abstract String namespace();

    public abstract String name();

    public static String namespaceOf(Object item) {
        if (item instanceof Enum<?>) {
            return ReflectionUtils.normalizeClassName(item.getClass());
        } else if (item instanceof Class) {
            return ReflectionUtils.normalizeClassName((Class<?>) item);
        } else if (item instanceof String) {
            String string = (String) item;
            if (string.contains(".")) {
                return string.substring(0, string.lastIndexOf("."));
            } else {
                return string;
            }
        } else {
            return ReflectionUtils.classParentName(item.toString());
        }
    }

    public static String nameOf(Object item) {
        String name;
        if (item instanceof Enum<?>) {
            name = ((Enum<?>) item).name();
        } else if (item instanceof String) {
            String string = (String) item;
            if (string.contains(".")) {
                return string.substring(string.lastIndexOf(".") + 1);
            } else {
                return string;
            }
        } else {
            name = item.toString();
        }
        return name;
    }

    public static QualifiedItem<?> fromType(Object value) {
        if (value instanceof QualifiedItem) {
            return (QualifiedItem<?>) value;
        } else if (value instanceof Enum<?>) {
            return new QualifiedEnum((Enum<?>) value);
        } else {
            return new QualifiedObject(value);
        }
    }

}
