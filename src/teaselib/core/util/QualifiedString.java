package teaselib.core.util;

import java.util.Optional;

import teaselib.core.StateImpl;
import teaselib.core.state.AbstractProxy;
import teaselib.core.state.ItemProxy;
import teaselib.util.ItemImpl;

public class QualifiedString {

    /**
     * Denotes the name of any item or state of the given name space, enumeration or class.
     */
    public static final String ANY = "*";

    public static QualifiedString from(QualifiedString kind, String guid) {
        return new QualifiedString(kind.namespace(), kind.name(), guid);
    }

    public static QualifiedString from(Enum<?> kind, String guid) {
        return QualifiedString.from(QualifiedString.of(kind), guid);
    }

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
            return item.guid;
        } else {
            throw new UnsupportedOperationException(object.toString());
        }
    }

    private final String value;
    private final String guid;

    public QualifiedString(String value) {
        this.value = valueWithoutGuid(value);
        if (this.value != value) {
            this.guid = value.substring(this.value.length() + 1);
        } else {
            this.guid = null;
        }
    }

    public QualifiedString(String namespace, String name) {
        this.value = ReflectionUtils.qualified(namespace, name);
        this.guid = null;
    }

    public QualifiedString(String namespace, String name, String guid) {
        this.value = ReflectionUtils.qualified(namespace, name);
        this.guid = guid;
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((guid == null) ? 0 : guid.toLowerCase().hashCode());
        result = prime * result + ((value == null) ? 0 : value.toLowerCase().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        QualifiedString other = (QualifiedString) obj;
        if (guid == null) {
            if (other.guid != null)
                return false;
        } else if (!guid.equalsIgnoreCase(other.guid))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equalsIgnoreCase(other.value))
            return false;
        return true;
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

    public boolean is(Object object) {
        return equals(QualifiedString.of(object));
    }

    public String value() {
        return value;
    }

    public Optional<String> guid() {
        return guid != null ? Optional.of(guid) : Optional.empty();
    }

    public static boolean isItemGuid(QualifiedString obj) {
        return obj.guid().isPresent();
    }

    public static boolean isItemGuid(Object obj) {
        return obj instanceof QualifiedString && ((QualifiedString) obj).guid().isPresent();
    }

    @Override
    public String toString() {
        if (guid != null) {
            return toString(value, guid);
        } else {
            return value;
        }
    }

    static String toString(String path, String guid) {
        return path + "#" + guid;
    }

}
