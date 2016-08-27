/**
 * 
 */
package teaselib;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import teaselib.TeaseLib.Duration;

/**
 * @author someone
 *
 */
public class State<T extends Enum<T>> {

    final TeaseLib teaseLib;
    final Map<T, Item> state = new HashMap<T, Item>();

    public State(TeaseLib teaseLib, Class<T> enumClass) {
        super();
        this.teaseLib = teaseLib;
        for (T value : values(enumClass)) {
            String args = teaseLib.getString(namespaceOf(value), nameOf(value));
            if (args != null) {
                String[] argv = args.split(" ");
                add(value, Long.parseLong(argv[0]), Long.parseLong(argv[1]));
            }
        }
    }

    private T[] values(Class<T> enumClass) {
        return (enumClass.getEnumConstants());
    }

    private String namespaceOf(T item) {
        return item.getClass().getName();
    }

    private String nameOf(T item) {
        return item.name();
    }

    public class Item {
        private final T item;
        Duration duration;
        private long expected;

        /**
         * Apply an item infinitely
         * 
         * @param item
         */
        Item(T item) {
            this(item, Long.MAX_VALUE, TimeUnit.SECONDS);
        }

        /**
         * Apply an item for a specific duration
         * 
         * @param item
         * @param time
         *            , TimeUnit unit The number of seconds to apply the item,
         *            or 0 to remove.
         */
        Item(T item, long time, TimeUnit unit) {
            this.item = item;
            apply(time, unit);
        }

        /**
         * Apply an item for a specific duration which has already begun.
         *
         * @param item
         * @param startTimeSeconds
         * @param howLongSeconds
         */
        private Item(T item, long startTimeSeconds, long howLongSeconds) {
            this.item = item;
            this.duration = teaseLib.new Duration(startTimeSeconds);
            this.expected = howLongSeconds;
            if (howLongSeconds > 0) {
                save();
            }
        }

        public Duration getDuration() {
            return duration;
        }

        public boolean valid() {
            return !expired();
        }

        public boolean expired() {
            return duration.elapsed(TimeUnit.SECONDS) >= expected;
        }

        /**
         * Time until the item expires.
         * 
         * @param unit
         *            THe unit of the return value.
         * @return The remaining time for the item.
         */
        public long remaining(TimeUnit unit) {
            long now = teaseLib.getTime(unit);
            long end = unit.convert(
                    duration.elapsedSeconds() + duration.start + expected,
                    TimeUnit.SECONDS);
            long remaining = Math.max(0, end - now);
            return remaining;
        }

        /**
         * @return Whether the item is currently applied.
         * 
         */
        public boolean applied() {
            return expected > 0;
        }

        public boolean freeSince(long time, TimeUnit unit) {
            return duration.elapsed(unit) >= time;
        }

        /**
         * Remove the item.
         */
        public void remove() {
            clear();
            State.this.remove(item);
        }

        /**
         * Clears the item.
         * <p>
         * Last usage including apply-duration is written to duration start, so
         * duration denotes the time the item was taken off.
         * <p>
         * As a result, for an item that is not applied, the duration elapsed
         * time is the duration the item hasn't been applied since the last
         * usage
         */
        private void clear() {
            duration = teaseLib.new Duration(
                    duration.start + duration.elapsed(TimeUnit.SECONDS));
            expected = 0;
            save();
        }

        public void apply() {
            apply(Long.MAX_VALUE, TimeUnit.SECONDS);
        }

        /**
         * Start a duration on the item. This clears any previous durations.
         * 
         * @param time
         * @param unit
         */
        public void apply(long time, TimeUnit unit) {
            this.duration = teaseLib.new Duration();
            this.expected = unit.toSeconds(time);
            save();
        }

        private void save() {
            if (expected > 0 && expected < Long.MAX_VALUE) {
                teaseLib.set(namespaceOf(item), nameOf(item),
                        persisted(duration.start, expected));
            } else {
                teaseLib.clear(namespaceOf(item), nameOf(item));
            }
        }
    }

    private static String persisted(long when, long howLongSeconds) {
        return when + " " + howLongSeconds;
    }

    public boolean has(T item) {
        return state.containsKey(item);
    }

    public Item add(T item) {
        Item value = new Item(item);
        state.put(item, value);
        return value;
    }

    public Item add(T item, long time, TimeUnit unit) {
        Item value = new Item(item, time, unit);
        state.put(item, value);
        return value;
    }

    Item add(T item, long startSeconds, long howLongSeconds) {
        Item value = new Item(item, startSeconds, howLongSeconds);
        state.put(item, value);
        return value;
    }

    public void remove(T item) {
        state.remove(item).clear();
    }

    public Item get(T item) {
        return state.get(item);
    }

    public Map<T, Item> expired() {
        Map<T, Item> items = new HashMap<T, Item>();
        for (Map.Entry<T, Item> entry : state.entrySet()) {
            State<T>.Item item = entry.getValue();
            if (item.expired()) {
                items.put(entry.getKey(), entry.getValue());
            }
        }
        return items;
    }

    public Map<T, Item> remaining() {
        Map<T, Item> items = new HashMap<T, Item>();
        for (Map.Entry<T, Item> entry : state.entrySet()) {
            State<T>.Item item = entry.getValue();
            if (!item.expired()) {
                items.put(entry.getKey(), entry.getValue());
            }
        }
        return items;
    }
}
