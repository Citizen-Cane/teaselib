package teaselib.core.state;

import teaselib.Duration;
import teaselib.core.ScriptEvents;
import teaselib.core.StateMaps;
import teaselib.util.Item;

public class ItemProxy extends AbstractProxy<Item> implements Item, StateMaps.Attributes {
    public final Item item;
    final ScriptEvents events;

    public ItemProxy(String namespace, Item item, ScriptEvents events) {
        super(namespace, item);
        this.item = state;
        this.events = events;
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
    public Options applyTo(Object... items) {
        injectNamespace();
        StateOptionsProxy options = new StateOptionsProxy(namespace, item.applyTo(items), events);
        events.itemApplied.run(new ScriptEvents.ItemChangedEventArgs(item));
        return options;
    }

    @Override
    public Options apply() {
        injectNamespace();
        StateOptionsProxy options = new StateOptionsProxy(namespace, item.apply(), events);
        events.itemApplied.run(new ScriptEvents.ItemChangedEventArgs(item));
        return options;
    }

    private void injectNamespace() {
        ((StateMaps.Attributes) item).applyAttributes(namespace);
    }

    @Override
    public void remove() {
        events.itemRemoved.run(new ScriptEvents.ItemChangedEventArgs(item));
        item.remove();
    }

    @Override
    public void removeFrom(Object... peers) {
        events.itemRemoved.run(new ScriptEvents.ItemChangedEventArgs(item));
        item.removeFrom(peers);
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