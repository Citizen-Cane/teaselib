/**
 * 
 */
package teaselib.core.state;

import teaselib.Duration;
import teaselib.core.StateMaps;
import teaselib.util.Item;

public class ItemProxy extends AbstractProxy<Item> implements Item, StateMaps.Attributes {
    public final Item item;

    public ItemProxy(String namespace, Item item) {
        super(namespace, item);
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
}