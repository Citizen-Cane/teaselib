/**
 * 
 */
package teaselib;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import teaselib.TeaseLib.Duration;

// todo global get without script

/**
 * @author someone
 *
 */
public class State<T extends Enum<T>> {

    final TeaseLib teaseLib;
    final Map<T, Item> state = new HashMap<T, Item>();

    public State(TeaseLib teaseLib, Class<Enum<?>> enumClass) {
        super();
        this.teaseLib = teaseLib;
        for (T value : values(enumClass)) {
            String args = teaseLib.getString(getPropertyName(value));
            if (args != null) {
                String[] argv = args.split(" ");
                add(value, Long.parseLong(argv[0]), Long.parseLong(argv[1]));
            }
        }
    }

    @SuppressWarnings({ "unchecked", "static-method" })
    private T[] values(Class<Enum<?>> enumClass) {
        return (T[]) enumClass.getEnumConstants();
    }

    public String getPropertyName(Enum<T> item) {
        return item.getClass().getName() + "." + item.name();
    }

    public void save(T item, Duration duration, long time, TimeUnit unit) {
        teaseLib.set(getPropertyName(item),
                persisted(duration.start, unit.toSeconds(time)));
    }

    public class Item {
        public final T item;
        public final Duration duration;
        public final long howLongSeconds;

        /**
         * Apply an item infinitely
         * 
         * @param item
         */
        public Item(T item) {
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
        public Item(T item, long time, TimeUnit unit) {
            super();
            this.item = item;
            this.duration = teaseLib.new Duration();
            this.howLongSeconds = unit.toSeconds(time);
            if (howLongSeconds > 0) {
                save(item, duration, time, unit);
            }
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
            this.howLongSeconds = howLongSeconds;
        }

        public boolean expired() {
            return duration.elapsed(TimeUnit.SECONDS) > howLongSeconds;
        }

        public boolean applied() {
            return howLongSeconds > 0;
        }

        public boolean freeSince(long time, TimeUnit unit) {
            return duration.elapsed(unit) >= time;
        }

        public void remove() {
            State.this.remove(item);
        }

        /**
         * Clears the item.
         * 
         * Last usage including apply-duration is written to duration start, so
         * duration denotes the time the item was taken off.
         * 
         * As a result, for an item that is not applied, the duration elapsed
         * time is the duration the item hasn't been applied since the last
         * usage
         */
        public void clear() {
            teaseLib.set(
                    getPropertyName(item),
                    persisted(
                            duration.start + duration.elapsed(TimeUnit.SECONDS),
                            0));
        }
    }

    private static String persisted(long when, long howLongSeconds) {
        return when + " " + howLongSeconds;
    }

    public boolean has(T item) {
        return state.containsKey(item);
    }

    public Item add(T item) {
        final Item value = new Item(item);
        state.put(item, value);
        return value;
    }

    public Item add(T item, long time, TimeUnit unit) {
        final Item value = new Item(item, time, unit);
        state.put(item, value);
        return value;
    }

    Item add(T item, long startSeconds, long howLongSeconds) {
        final Item value = new Item(item, startSeconds, howLongSeconds);
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
