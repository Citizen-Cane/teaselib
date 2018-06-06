package teaselib.core.util;

import teaselib.State;
import teaselib.core.StateImpl;
import teaselib.core.state.StateProxy;
import teaselib.util.Item;

// TODO Merge with QualifiedItemImpl (it's duplicated code for now)
class QualifiedStateImpl extends AbstractQualifiedItem<State> {

    public QualifiedStateImpl(State value) {
        super(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        else if (this == obj)
            return true;
        else if (obj instanceof Item) {
            Item other = (Item) obj;
            return value.equals(other);
        } else if (obj instanceof QualifiedStateImpl) {
            QualifiedStateImpl other = (QualifiedStateImpl) obj;
            if (value == null) {
                return other.value == null;
            } else {
                return value.equals(other.value);
            }
        } else if (obj instanceof Enum<?> || obj instanceof String) {
            return QualifiedItem.of(state(value)).equals(QualifiedItem.of(obj));
        } else if (obj instanceof Enum<?> || obj instanceof QualifiedItem) {
            return QualifiedItem.of(state(value)).equals(obj);
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
        return QualifiedItem.of(state(value)).namespace();
    }

    @Override
    public String name() {
        return QualifiedItem.of(state(value)).name();
    }

    @Override
    public String toString() {
        return QualifiedItem.of(state(value)).toString();
    }

    private static Object state(State value) {
        if (value instanceof StateProxy) {
            return ((StateImpl) ((StateProxy) value).state).item;
        } else if (value instanceof StateImpl) {
            return ((StateImpl) value).item;
        } else {
            throw new UnsupportedOperationException(value.toString());
        }
    }
}
