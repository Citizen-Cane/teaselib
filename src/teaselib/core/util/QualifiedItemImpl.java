package teaselib.core.util;

import java.util.Optional;

import teaselib.core.state.AbstractProxy;
import teaselib.util.Item;

class QualifiedItemImpl extends AbstractQualifiedItem<Item> {

    public QualifiedItemImpl(Item value) {
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
        } else if (obj instanceof QualifiedItemImpl) {
            QualifiedItemImpl other = (QualifiedItemImpl) obj;
            if (value == null) {
                return other.value == null;
            } else {
                return value.equals(other.value);
            }
        } else if (obj instanceof Enum<?> || obj instanceof String) {
            return QualifiedItem.of(item(value)).equals(QualifiedItem.of(obj));
        } else if (obj instanceof Enum<?> || obj instanceof QualifiedItem) {
            return QualifiedItem.of(item(value)).equals(obj);
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
        return QualifiedItem.of(item(value)).namespace();
    }

    @Override
    public String name() {
        return QualifiedItem.of(item(value)).name();
    }

    @Override
    public Optional<String> guid() {
        return Optional.of(AbstractProxy.itemImpl(value).guid.name());
    }

    @Override
    public String toString() {
        return toString(QualifiedItem.of(item(value)).toString(), guid());
    }

    private static Object item(Item item) {
        return AbstractProxy.itemImpl(item).value;
    }
}
