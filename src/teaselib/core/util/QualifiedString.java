package teaselib.core.util;

import java.util.Optional;

public class QualifiedString extends AbstractQualifiedItem<String> {

    public static QualifiedString of(Class<?> clazz) {
        return new QualifiedString(ReflectionUtils.qualified(clazz), "*");
    }

    public static QualifiedString of(Object object) {
        if (object instanceof QualifiedString) {
            return (QualifiedString) object;
        } else if (object instanceof Class<?>) {
            return QualifiedString.of((Class<?>) object);
        } else {
            return new QualifiedString(QualifiedItem.of(object).toString());
        }
    }

    private final Optional<String> guid;

    public QualifiedString(String value) {
        super(valueWithoutGuid(value));
        if (this.value != value) {
            this.guid = Optional.of(value.substring(this.value.length() + 1));
        } else {
            this.guid = Optional.empty();
        }
    }

    public QualifiedString(String namespace, String name) {
        super(ReflectionUtils.qualified(namespace, name));
        this.guid = Optional.empty();
    }

    public QualifiedString(String namespace, String name, String guid) {
        super(ReflectionUtils.qualified(namespace, name));
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

    @Override
    public String namespace() {
        return QualifiedItem.namespaceOf(value);
    }

    @Override
    public String name() {
        return QualifiedItem.nameOf(value);
    }

    @Override
    public Optional<String> guid() {
        return guid;
    }

    @Override
    public String toString() {
        if (guid.isPresent()) {
            return toString(value, guid);
        } else {
            return value;
        }
    }

}
