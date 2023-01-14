package teaselib.core.state;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Duration;
import teaselib.State;
import teaselib.core.ItemImpl;
import teaselib.core.ItemLogger;
import teaselib.core.ScriptEvents;
import teaselib.core.ScriptEvents.ItemChangedEventArgs;
import teaselib.core.util.QualifiedString;
import teaselib.util.Item;

public class ItemProxy extends AbstractProxy<Item> implements Item, State.Attributes {

    private static final Logger logger = LoggerFactory.getLogger(ItemProxy.class);

    public final Item item;
    public final ScriptEvents events;

    private final ItemLogger itemLogger;

    public ItemProxy(String namespace, Item item, ScriptEvents events) {
        super(namespace, item);
        this.item = state;
        this.events = events;
        this.itemLogger = ((ItemImpl) item).teaseLib.itemLogger;
    }

    @Override
    public boolean isAvailable() {
        boolean available = item.isAvailable();
        itemLogger.log(item, "isAvailable", available);
        return available;
    }

    @Override
    public void setAvailable(boolean isAvailable) {
        item.setAvailable(isAvailable);
        itemLogger.log(item, "setAvailable", isAvailable);
    }

    @Override
    public String displayName() {
        return item.displayName();
    }

    @Override
    public boolean is(Object... attributes) {
        boolean is = item.is(attributes);
        itemLogger.log(item, "is", attributes, is);
        return is;
    }

    static final class ItemEventProxy implements State.Options {
        final Item item;
        final ItemOptionsProxy options;
        private ScriptEvents events;

        private final ItemLogger itemLogger;

        public ItemEventProxy(Item item, ScriptEvents events, ItemOptionsProxy options) {
            this.item = item;
            this.events = events;
            this.options = options;
            this.itemLogger = ((ItemImpl) item).teaseLib.itemLogger;
        }

        @Override
        public void remember(Until forget) {
            options.remember(forget);
            itemLogger.log(item, "remember", forget);
            events.itemRemember.fire(new ItemChangedEventArgs(item));
        }

        @Override
        public Persistence over(long duration, TimeUnit unit) {
            var persistence = options.over(duration, unit);
            itemLogger.log(item, "over", duration, unit);
            events.itemDuration.fire(new ItemChangedEventArgs(item));
            return persistence;
        }

        @Override
        public Persistence over(Duration duration) {
            var persistence = options.over(duration);
            itemLogger.log(item, "over", duration);
            events.itemDuration.fire(new ItemChangedEventArgs(item));
            return persistence;
        }
    }

    @Override
    public Options applyTo(Object... items) {
        injectNamespace();

        var options = new ItemOptionsProxy((ItemImpl) item, namespace, item.applyTo(items), events, itemLogger);
        itemLogger.log(item, "applyTo", items);

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

        var options = new ItemOptionsProxy((ItemImpl) item, namespace, item.apply(), events, itemLogger);
        itemLogger.log(item, "apply");

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

        itemLogger.log(item, "remove");

        // TODO event after state change statement or rename to beforeItemRemove
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
        // TODO event after state change statement
        events.itemRemoved.fire(new ScriptEvents.ItemChangedEventArgs(item));
        itemLogger.log(item, "removeFrom");
        item.removeFrom(peers);
    }

    @Override
    public boolean canApply() {
        boolean canApply = item.canApply();
        itemLogger.log(item, "canApply", canApply);
        return canApply;
    }

    @Override
    public Item to(Object... additionalPeers) {
        return new ItemProxy(namespace, item, events) {
            @Override
            public Options apply() {
                itemLogger.log(item, "apply to");
                Options options = super.apply();
                super.applyTo(additionalPeers);
                return options;
            }

            @Override
            public boolean canApply() {
                itemLogger.log(item, "canApply to");
                return super.canApply() && Arrays.stream(additionalPeers).noneMatch(
                        additionalPeer -> itemImpl(item).state(QualifiedString.of(additionalPeer)).applied());
            }
        };
    }

    @Override
    public boolean applied() {
        boolean applied = item.applied();
        itemLogger.log(item, "applied", applied);
        return applied;
    }

    @Override
    public boolean expired() {
        boolean expired = item.expired();
        itemLogger.log(item, "expired", expired);
        return expired;
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
        long removed = item.removed(unit);
        itemLogger.log(item, "removed", removed, unit);
        return removed;
    }

    @Override
    public void applyAttributes(Object... attributes) {
        ((State.Attributes) item).applyAttributes(attributes);
    }

}
