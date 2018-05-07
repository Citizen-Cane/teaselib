package teaselib.core.util;

/**
 * @author Citizen-Cane
 *
 */
public abstract class QualifiedItem {

    public abstract boolean is(Object obj);

    public abstract String namespace();

    public abstract String name();

    public abstract Object value();

    public static String namespaceOf(Object item) {
        if (item instanceof Enum<?>) {
            return ReflectionUtils.normalizeClassName(item.getClass());
        } else if (item instanceof Class) {
            return ReflectionUtils.normalizeClassName((Class<?>) item);
        } else if (item instanceof String) {
            String string = (String) item;
            if (string.contains(".")) {
                return string.substring(0, string.lastIndexOf('.'));
            } else {
                return string;
            }
        } else if (item instanceof QualifiedItem) {
            return ((QualifiedItem) item).namespace();
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
                return string.substring(string.lastIndexOf('.') + 1);
            } else {
                return string;
            }
        } else if (item instanceof AbstractQualifiedItem<?>) {
            return ((QualifiedItem) item).name();
        } else {
            name = item.toString();
        }
        return name;
    }

    public static QualifiedItem of(Object value) {
        if (value instanceof AbstractQualifiedItem) {
            return (QualifiedItem) value;
        } else if (value instanceof Enum<?>) {
            return new QualifiedEnum((Enum<?>) value);
        } else {
            return new QualifiedObject(value);
        }
    }

}