/**
 * 
 */
package teaselib.core.state;

import teaselib.Duration;
import teaselib.core.StateMaps;
import teaselib.util.Item;

public class ItemProxy implements Item, StateMaps.Attributes {
    final String namespace;
    final Item item;

    public ItemProxy(String namespace, Item item) {
        this.namespace = namespace;
        this.item = item;
    }

    @Override
    public boolean isAvailable() {
        return item.isAvailable();
    }

    @Override
    public void setAvailable(boolean isAvailable) {
        item.setAvailable(isAvailable);
    }

    @Override
    public String displayName() {
        return item.displayName();
    }

    @Override
    public boolean is(Object... attributes) {
        return item.is(attributes);
    }

    @Override
    public <S> Options applyTo(S... items) {
        injectNamespace();
        return new StateOptionsProxy(namespace, item.applyTo(items));
    }

    @Override
    public Options apply() {
        injectNamespace();
        return new StateOptionsProxy(namespace, item.apply());
    }

    private void injectNamespace() {
        ((StateMaps.Attributes) item).applyAttributes(namespace);
    }

    @Override
    public Persistence remove() {
        return new StatePersistenceProxy(namespace, item.remove());
    }

    @Override
    public <S extends Object> Persistence removeFrom(S... peer) {
        return new StatePersistenceProxy(namespace, item.removeFrom(peer));
    }

    @Override
    public boolean canApply() {
        return item.canApply();
    }

    @Override
    public boolean applied() {
        return item.applied();
    }

    @Override
    public boolean expired() {
        return item.expired();
    }

    @Override
    public Duration duration() {
        return item.duration();
    }

    @Override
    public void applyAttributes(Object... attributes) {
        ((StateMaps.Attributes) item).applyAttributes(attributes);
    }

    @Override
    public String toString() {
        return item.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((item == null) ? 0 : item.hashCode());
        result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
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
        ItemProxy other = (ItemProxy) obj;
        if (item == null) {
            if (other.item != null)
                return false;
        } else if (!item.equals(other.item))
            return false;
        if (namespace == null) {
            if (other.namespace != null)
                return false;
        } else if (!namespace.equals(other.namespace))
            return false;
        return true;
    }

}