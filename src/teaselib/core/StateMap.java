package teaselib.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import teaselib.State;
import teaselib.core.TeaseLib.Duration;
import teaselib.core.TeaseLib.PersistentString;
import teaselib.core.util.Persist;

/**
 * State-Implementation;
 * <li>A mapping from item class to item.
 * <li>An implementation of the {@link State} interface.
 * 
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
        private final Object item;
        Object what = None;

        Duration duration;
        private long expectedSeconds;

        private StateImpl(T item) {
            this.storage = storage(item);
            this.item = item;
            if (storage.available()) {
                String[] argv = storage.value().split(" ");
                long startSeconds = Long.parseLong(argv[0]);
                long howLongSeconds = Long.parseLong(argv[1]);
                this.duration = teaseLib.new Duration(startSeconds);
                this.expectedSeconds = howLongSeconds;
                this.what = Persist.from(argv[2]);
            } else {
                // TODO should start at REMOVED, and expected should be 0
                this.duration = teaseLib.new Duration(0);
                this.expectedSeconds = REMOVED;
                this.what = None;
            }
        }

        private PersistentString storage(T value) {
            return teaseLib.new PersistentString(TeaseLib.DefaultDomain,
                    namespaceOf(value), nameOf(value));
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
        public State remove() {
            duration = teaseLib.new Duration(
                    duration.startSeconds + duration.elapsed(TimeUnit.SECONDS));
            expectedSeconds = REMOVED;
            removePersistenceeButKeepCached();
            return this;
        }

        private void removePersistenceeButKeepCached() {
            storage.clear();
        }

        @Override
        public <W> State apply(W what) {
            apply(what, TEMPORARY, TimeUnit.SECONDS);
            return this;
        }

        @Override
        public <W> State apply(W what, long duration, TimeUnit unit) {
            return apply(what, teaseLib.getTime(unit), duration, unit);
        }

        @Override
        public <W> State apply(W what, long time, long duration,
                TimeUnit unit) {
            this.what = what;
            this.duration = teaseLib.new Duration();
            this.expectedSeconds = unit.toSeconds(duration);
            update();
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <I> I item() {
            return (I) item;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <W> W what() {
            return (W) what;
        }

        private void update() {
            if (storage.available()) {
                remember();
            }
        }

        @Override
        public State remember() {
            storage.set(persisted());
            return this;
        }

        private String persisted() {
            return duration.startSeconds + " " + expectedSeconds + " "
                    + Persist.to(what);
        }
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
