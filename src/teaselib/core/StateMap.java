/**
 * 
 */
package teaselib.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import teaselib.State;
import teaselib.core.TeaseLib.Duration;

/**
 * @author someone
 *
 */
public class StateMap<T extends Enum<T>> {

    final TeaseLib teaseLib;
    final Map<T, StateImpl> state = new HashMap<T, StateImpl>();

    public StateMap(TeaseLib teaseLib, Class<T> enumClass) {
        super();
        this.teaseLib = teaseLib;
        for (T value : values(enumClass)) {
            String args = teaseLib.getString(namespaceOf(value), nameOf(value));
            if (args != null) {
                String[] argv = args.split(" ");
                long startSeconds = Long.parseLong(argv[0]);
                long howLongSeconds = Long.parseLong(argv[1]);
                StateImpl state = new StateImpl(value, startSeconds,
                        howLongSeconds);
                add(state);
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

    private class StateImpl implements State {
        private final T item;
        Duration duration;
        private long expectedSeconds;

        /**
         * Apply an item for a specific duration which has already begun.
         *
         * @param item
         * @param startTimeSeconds
         * @param expectedSeconds
         */
        private StateImpl(T item, long startTimeSeconds, long expectedSeconds) {
            this.item = item;
            this.duration = teaseLib.new Duration(startTimeSeconds);
            this.expectedSeconds = expectedSeconds;
            if (expectedSeconds > 0) {
                save();
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see teaselib.core.StateItem#getDuration()
         */
        @Override
        public Duration getDuration() {
            return duration;
        }

        /*
         * (non-Javadoc)
         * 
         * @see teaselib.core.StateItem#valid()
         */
        @Override
        public boolean valid() {
            return !expired();
        }

        /*
         * (non-Javadoc)
         * 
         * @see teaselib.core.StateItem#expired()
         */
        @Override
        public boolean expired() {
            return duration.elapsed(TimeUnit.SECONDS) >= expectedSeconds;
        }

        @Override
        public long expected() {
            return expectedSeconds;
        }

        /*
         * (non-Javadoc)
         * 
         * @see teaselib.core.StateItem#remaining(java.util.concurrent.TimeUnit)
         */
        @Override
        public long remaining(TimeUnit unit) {
            long now = teaseLib.getTime(unit);
            long end = unit.convert(duration.elapsedSeconds()
                    + duration.startSeconds + expectedSeconds,
                    TimeUnit.SECONDS);
            long remaining = Math.max(0, end - now);
            return remaining;
        }

        @Override
        public boolean applied() {
            return expectedSeconds > 0;
        }

        @Override
        public boolean freeSince(long time, TimeUnit unit) {
            return duration.elapsed(unit) >= time;
        }

        @Override
        public void remove() {
            clear();
            StateMap.this.remove(item);
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
                    duration.startSeconds + duration.elapsed(TimeUnit.SECONDS));
            expectedSeconds = 0;
            save();
        }

        @Override
        public void apply() {
            apply(Long.MAX_VALUE, TimeUnit.SECONDS);
        }

        @Override
        public void apply(long time, TimeUnit unit) {
            this.duration = teaseLib.new Duration();
            this.expectedSeconds = unit.toSeconds(time);
            save();
        }

        private void save() {
            if (expectedSeconds > 0 && expectedSeconds < Long.MAX_VALUE) {
                teaseLib.set(namespaceOf(item), nameOf(item),
                        persisted(duration.startSeconds, expectedSeconds));
            } else {
                teaseLib.clear(namespaceOf(item), nameOf(item));
            }
        }
    }

    private static String persisted(long whenSeconds, long howLongSeconds) {
        return whenSeconds + " " + howLongSeconds;
    }

    public boolean has(T item) {
        return state.containsKey(item);
    }

    State add(T item, long startSeconds, long howLongSeconds) {
        StateImpl value = new StateImpl(item, startSeconds, howLongSeconds);
        state.put(item, value);
        return value;
    }

    State add(StateImpl item) {
        state.put(item.item, item);
        return item;
    }

    public void remove(T item) {
        if (has(item)) {
            StateMap<T>.StateImpl removed = state.remove(item);
            if (removed != null) {
                removed.clear();
            }
        }
    }

    public State get(T item) {
        return state.get(item);
    }

    public Map<T, StateImpl> expired() {
        Map<T, StateImpl> items = new HashMap<T, StateImpl>();
        for (Map.Entry<T, StateImpl> entry : state.entrySet()) {
            StateImpl item = entry.getValue();
            if (item.expired()) {
                items.put(entry.getKey(), entry.getValue());
            }
        }
        return items;
    }

    public Map<T, StateImpl> remaining() {
        Map<T, StateImpl> items = new HashMap<T, StateImpl>();
        for (Map.Entry<T, StateImpl> entry : state.entrySet()) {
            StateImpl item = entry.getValue();
            if (!item.expired()) {
                items.put(entry.getKey(), entry.getValue());
            }
        }
        return items;
    }
}
