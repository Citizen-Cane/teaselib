package teaselib.core.state;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Duration;
import teaselib.State;
import teaselib.core.ScriptEvents;
import teaselib.core.ScriptEvents.ItemChangedEventArgs;
import teaselib.util.Item;

public class ItemProxy extends AbstractProxy<Item> implements Item, State.Attributes {
    private static final Logger logger = LoggerFactory.getLogger(ItemProxy.class);

    public final Item item;
    public final ScriptEvents events;

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
            events.itemRemember.fire(new ItemChangedEventArgs(item));
        }

        @Override
        public Persistence over(long duration, TimeUnit unit) {
            var persistence = options.over(duration, unit);
            events.itemDuration.fire(new ItemChangedEventArgs(item));
            return persistence;
        }

        @Override
        public Persistence over(Duration duration) {
            var persistence = options.over(duration);
            events.itemDuration.fire(new ItemChangedEventArgs(item));
            return persistence;
        }
    }

    @Override
    public Options applyTo(Object... items) {
        injectNamespace();
        StateOptionsProxy options = new StateOptionsProxy(namespace, item.applyTo(items), events);
        events.itemApplied.fire(new ScriptEvents.ItemChangedEventArgs(item));
        return new ItemEventProxy(item, events, options);
    }

    @Override
    public Options apply() {
        if (applied()) {
            String humanReadableItem = item.displayName() + "(id="
                    + AbstractProxy.itemImpl(item).name.guid().orElseThrow() + ")";
            if (is(namespace)) {
                String message = humanReadableItem + " already applied";
                report(message);
            } else {
                logger.warn("{} has already been applied in another namespace", humanReadableItem);
            }
        }

        injectNamespace();
        StateOptionsProxy options = new StateOptionsProxy(namespace, item.apply(), events);
        events.itemApplied.fire(new ScriptEvents.ItemChangedEventArgs(item));
        return new ItemEventProxy(item, events, options);
    }

    private void injectNamespace() {
        ((State.Attributes) item).applyAttributes(namespace);
    }

    @Override
    public void remove() {
        if (!applied()) {
            String message = AbstractProxy.itemImpl(item).name + " is not applied";
            report(message);
        }

        events.itemRemoved.fire(new ScriptEvents.ItemChangedEventArgs(item));
        item.remove();
    }

    private static void report(String message) {
        StringBuilder result = new StringBuilder(message);
        String newLine = System.getProperty("line.separator");
        result.append(newLine);
        result.append(stream(Thread.currentThread().getStackTrace()).map(Objects::toString).collect(joining(newLine)));
        logger.warn("{}", result);
    }

    @Override
    public void removeFrom(Object... peers) {
        events.itemRemoved.fire(new ScriptEvents.ItemChangedEventArgs(item));
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
    public boolean removed() {
        return item.removed();
    }

    @Override
    public long removed(TimeUnit unit) {
        return item.removed(unit);
    }

    @Override
    public void applyAttributes(Object... attributes) {
        ((State.Attributes) item).applyAttributes(attributes);
    }

}
