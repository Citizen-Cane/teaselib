package teaselib.core.util;

import java.util.Optional;

import teaselib.core.StateImpl;
import teaselib.core.state.AbstractProxy;
import teaselib.core.state.ItemProxy;
import teaselib.util.ItemGuid;
import teaselib.util.ItemImpl;

// TODO remove all occurrences of QualifiedItem sub-classes, then remove super class
public class QualifiedString extends AbstractQualifiedItem<String> {

    /**
     * Denotes the name of any item or state of the given name space, enumeration or class.
     */
    public static final String ANY = "*";

    public static QualifiedString of(Class<?> clazz) {
        return new QualifiedString(ReflectionUtils.qualified(clazz), ANY);
    }

    public static QualifiedString of(Object object) {
        if (object instanceof String) {
            return new QualifiedString((String) object);
        } else if (object instanceof Enum<?>) {
            Enum<?> item = (Enum<?>) object;
            return new QualifiedString(ReflectionUtils.qualifiedName(item));
        } else if (object instanceof QualifiedString) {
            return (QualifiedString) object;
        } else if (object instanceof Class<?>) {
            return QualifiedString.of((Class<?>) object);
        } else if (object instanceof StateImpl) {
            StateImpl state = (StateImpl) object;
            return state.item;
        } else if (object instanceof ItemProxy) {
            return of(AbstractProxy.removeProxy((ItemProxy) object));
        } else if (object instanceof ItemImpl) {
            ItemImpl item = (ItemImpl) object;
            return item.guid.item();
        } else if (object instanceof ItemGuid) {
            return ((ItemGuid) object).item();
        } else {
            throw new UnsupportedOperationException(object.toString());
        }
    }

    private final String value;
    private final Optional<String> guid;

    public QualifiedString(String value) {
        super(value);
        this.value = valueWithoutGuid(value);
        if (this.value != value) {
            this.guid = Optional.of(value.substring(this.value.length() + 1));
        } else {
            this.guid = Optional.empty();
        }
    }

    public QualifiedString(String namespace, String name) {
        super("x");
        this.value = ReflectionUtils.qualified(namespace, name);
        this.guid = Optional.empty();
    }

    public QualifiedString(String namespace, String name, String guid) {
        super("x");
        this.value = ReflectionUtils.qualified(namespace, name);
        this.guid = Optional.of(guid);
    }

    private static String valueWithoutGuid(String value) {
        int index = value.lastIndexOf('#');
        if (index >= 0) {
            return value.substring(0, index);
        } else {
            return value;
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
                return value.equalsIgnoreCase(other.value.toString());
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

    public String namespace() {
        if (value.contains(".")) {
            return value.substring(0, value.lastIndexOf('.'));
        } else {
            return value;
        }
    }

    public String name() {
        if (value.contains(".")) {
            return value.substring(value.lastIndexOf('.') + 1);
        } else {
            return value;
        }
    }

    public QualifiedString kind() {
        return new QualifiedString(namespace(), name());
    }

    public boolean is(Object obj) {
        return equals(obj);
    }

    public String value() {
        return value;
    }

    public Optional<String> guid() {
        return guid;
    }

    @Override
    public String toString() {
        if (guid.isPresent()) {
            return toString_(value, guid);
        } else {
            return value;
        }
    }

    static String toString_(String path, Optional<String> guid) {
        if (guid.isPresent()) {
            return path + "#" + guid.get();
        } else {
            return path;
        }
    }

}
