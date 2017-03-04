package teaselib.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import teaselib.State;
import teaselib.core.TeaseLib.Duration;
import teaselib.core.TeaseLib.PersistentString;

/**
 * @author Citizen-Cane
 *
 */
public class StateMap<T extends Enum<T>> {
    final TeaseLib teaseLib;
    final Map<T, StateImpl> state = new HashMap<T, StateImpl>();

    public StateMap(TeaseLib teaseLib) {
        super();
        this.teaseLib = teaseLib;
    }

    private void cacheItem(T value) {
        if (!state.containsKey(value)) {
            state.put(value, new StateImpl(value));
        }
    }

    private String namespaceOf(T item) {
        return item.getClass().getName();
    }

    private String nameOf(T item) {
        return item.name() + ".state";
    }

    private class StateImpl implements State {
        private final PersistentString storage;
        Duration duration;
        private long expectedSeconds;

        private StateImpl(T item) {
            this.storage = storage(item);
            if (storage.available()) {
                String[] argv = storage.value().split(" ");
                long startSeconds = Long.parseLong(argv[0]);
                long howLongSeconds = Long.parseLong(argv[1]);
                this.duration = teaseLib.new Duration(startSeconds);
                this.expectedSeconds = howLongSeconds;
            } else {
                this.duration = teaseLib.new Duration(0);
                this.expectedSeconds = REMOVED;
            }
        }

        private PersistentString storage(T value) {
            return teaseLib.new PersistentString(TeaseLib.DefaultDomain,
                    namespaceOf(value), nameOf(value));
        }

        /**
         * Apply an item for a specific duration which has already begun.
         *
         * @param item
         * @param startTimeSeconds
         * @param expectedSeconds
         */
        private StateImpl(T item, long startTimeSeconds, long expectedSeconds) {
            this.storage = storage(item);
            this.duration = teaseLib.new Duration(startTimeSeconds);
            this.expectedSeconds = expectedSeconds;
            update();
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
            return expectedSeconds >= 0;
        }

        @Override
        public boolean freeSince(long time, TimeUnit unit) {
            // TODO Check and test implementation
            return duration.elapsed(unit) >= time;
        }

        @Override
        public void remove() {
            duration = teaseLib.new Duration(
                    duration.startSeconds + duration.elapsed(TimeUnit.SECONDS));
            expectedSeconds = REMOVED;
            removePersistenceeButKeepCached();
        }

        private void removePersistenceeButKeepCached() {
            storage.clear();
        }

        @Override
        public void apply() {
            apply(0, TimeUnit.SECONDS);
        }

        @Override
        public void apply(long time, TimeUnit unit) {
            this.duration = teaseLib.new Duration();
            this.expectedSeconds = unit.toSeconds(time);
            update();
        }

        private void update() {
            if (storage.available()) {
                remember();
            }
        }

        @Override
        public void remember() {
            storage.set(persisted(duration.startSeconds, expectedSeconds));
        }
    }

    private static String persisted(long whenSeconds, long howLongSeconds) {
        return whenSeconds + " " + howLongSeconds;
    }

    State add(T item, long startSeconds, long howLongSeconds) {
        StateImpl value = new StateImpl(item, startSeconds, howLongSeconds);
        state.put(item, value);
        return value;
    }

    public State get(T item) {
        cacheItem(item);
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
