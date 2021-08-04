package teaselib.core.util;

import java.util.Optional;

import teaselib.State;
import teaselib.util.Item;

/**
 * @author Citizen-Cane
 *
 */
public interface QualifiedItem {

    public boolean is(Object obj);

    public String namespace();

    public String name();

    public Object value();

    public Optional<String> guid();

    public static String namespaceOf(Object item) {
        if (item instanceof Enum<?>) {
            return ReflectionUtils.qualified(item.getClass());
        } else if (item instanceof Class) {
            return ReflectionUtils.qualified((Class<?>) item);
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
            var string = (String) item;
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
        if (value instanceof QualifiedItem) {
            return (QualifiedItem) value;
        } else if (value instanceof Enum) {
            return new QualifiedEnum((Enum<?>) value);
        } else if (value instanceof Item) {
            return new QualifiedItemImpl((Item) value);
        } else if (value instanceof State) {
            return new QualifiedStateImpl((State) value);
        } else if (value instanceof String) {
            return new QualifiedString((String) value);
        } else {
            return new QualifiedObject(value);
        }
    }
}