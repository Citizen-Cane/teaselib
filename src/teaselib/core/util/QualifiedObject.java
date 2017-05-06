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
                return value.equals(other.value);
            }
        } else if (obj instanceof Enum<?>) {
            return value.toString().equals(QualifiedEnum.toString((Enum<?>) obj));
        } else {
            return this.toString().equals(obj.toString());
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
