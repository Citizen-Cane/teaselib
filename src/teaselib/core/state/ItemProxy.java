package teaselib.core.state;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Duration;
import teaselib.State;
import teaselib.core.ScriptEvents;
import teaselib.core.ScriptEvents.ItemChangedEventArgs;
import teaselib.core.StateMaps;
import teaselib.util.Item;

public class ItemProxy extends AbstractProxy<Item> implements Item, StateMaps.Attributes {
    private static final Logger logger = LoggerFactory.getLogger(ItemProxy.class);

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

    static final class ItemEventProxy implements State.Options {
        final Item item;
        final StateOptionsProxy options;
        private ScriptEvents events;

        public ItemEventProxy(Item item, ScriptEvents events, StateOptionsProxy options) {
            this.item = item;
            this.events = events;
            this.options = options;
        }

        @Override
        public void remember(Until forget) {
            options.remember(forget);
        }

        @Override
        public Persistence over(long duration, TimeUnit unit) {
            Persistence persistence = options.over(duration, unit);
            events.itemDuration.run(new ItemChangedEventArgs(item));
            return persistence;
        }

        @Override
        public Persistence over(Duration duration) {
            Persistence persistence = options.over(duration);
            events.itemDuration.run(new ItemChangedEventArgs(item));
            return persistence;
        }
    }

    @Override
    public Options applyTo(Object... items) {
        injectNamespace();
        StateOptionsProxy options = new StateOptionsProxy(namespace, item.applyTo(items), events);
        events.itemApplied.run(new ScriptEvents.ItemChangedEventArgs(item));
        return new ItemEventProxy(item, events, options);
    }

    @Override
    public Options apply() {
        if (applied()) {
            if (is(namespace)) {
                throw new IllegalStateException(AbstractProxy.itemImpl(item).guid + " has already been applied");
            } else {
                logger.warn("{} has already been applied in another namespace", AbstractProxy.itemImpl(item).guid);
            }
        }

        injectNamespace();
        StateOptionsProxy options = new StateOptionsProxy(namespace, item.apply(), events);
        events.itemApplied.run(new ScriptEvents.ItemChangedEventArgs(item));
        return new ItemEventProxy(item, events, options);
    }

    private void injectNamespace() {
        ((StateMaps.Attributes) item).applyAttributes(namespace);
    }

    @Override
    public void remove() {
        if (!applied()) {
            throw new IllegalStateException(AbstractProxy.itemImpl(item).guid + " is not applied");
        }

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