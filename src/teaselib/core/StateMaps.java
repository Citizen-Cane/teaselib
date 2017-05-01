package teaselib.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import teaselib.Duration;
import teaselib.State;
import teaselib.core.TeaseLib.PersistentString;
import teaselib.core.util.Persist;

public class StateMaps {
    final TeaseLib teaseLib;
    final Map<Class<?>, StateMap<? extends Object>> stateMaps = new HashMap<Class<?>, StateMap<? extends Object>>();

    public StateMaps(TeaseLib teaseLib) {
        this.teaseLib = teaseLib;
    }

    void clear() {
        stateMaps.clear();
    }

    public class StateMap<T extends Object> {
        final Map<T, State> states = new HashMap<T, State>();

        public State get(T item) {
            if (states.containsKey(item)) {
                return states.get(item);
            } else {
                State state = new StateImpl<T>(item);
                states.put(item, state);
                return state;
            }
        }

        void clear() {
            states.clear();
        }
    }

    public class StateImpl<T extends Object> implements State, State.Options {
        private static final String TEMPORARY_KEYWORD = "TEMPORARY";
        private static final String REMOVED_KEYWORD = "REMOVED";
        private static final String INDEFINITELY_KEYWORD = "INDEFINITELY";

        private final T item;
        private final PersistentString durationStorage;
        private final PersistentString peerStorage;

        private Duration duration = teaseLib.new DurationImpl(0, REMOVED, TimeUnit.SECONDS);
        private final Set<Object> peers = new HashSet<Object>();

        public StateImpl(T item) {
            super();
            this.item = item;
            this.durationStorage = teaseLib.new PersistentString(TeaseLib.DefaultDomain,
                    namespaceOf(item), nameOf(item) + "." + "duration");
            this.peerStorage = teaseLib.new PersistentString(TeaseLib.DefaultDomain,
                    namespaceOf(item), nameOf(item) + "." + "peers");
            restoreDuration();
            restorePeers();
        }

        private void restoreDuration() {
            if (durationStorage.available()) {
                String[] argv = durationStorage.value().split(" ");
                long start = Long.parseLong(argv[0]);
                long limit = string2limit(argv[1]);
                this.duration = teaseLib.new DurationImpl(start, limit, TimeUnit.SECONDS);
            }
        }

        private long string2limit(String limitString) {
            long limit;
            if (limitString.equals(REMOVED_KEYWORD)) {
                limit = REMOVED;
            } else if (limitString.equals(TEMPORARY_KEYWORD)) {
                limit = TEMPORARY;
            } else if (limitString.equals(INDEFINITELY_KEYWORD)) {
                limit = INDEFINITELY;
            } else {
                limit = Long.parseLong(limitString);
            }
            return limit;
        }

        private void restorePeers() {
            if (peerStorage.available()) {
                String[] serializedPeers = peerStorage.value()
                        .split(Persist.PERSISTED_STRING_SEPARATOR);
                for (String serializedPeer : serializedPeers) {
                    peers.add(Persist.from(serializedPeer));
                }
            }
        }

        private void persistDuration() {
            String startValue = Long.toString(duration.start(TimeUnit.SECONDS));
            long limit = duration.limit(TimeUnit.SECONDS);
            String limitValue = limit2String(limit);
            durationStorage.set(startValue + " " + limitValue);

        }

        private String limit2String(long limit) {
            String limitString;
            if (limit <= REMOVED) {
                limitString = REMOVED_KEYWORD;
            } else if (limit == TEMPORARY) {
                limitString = TEMPORARY_KEYWORD;
            } else if (limit == INDEFINITELY) {
                limitString = INDEFINITELY_KEYWORD;
            } else {
                limitString = Long.toString(duration.limit(TimeUnit.SECONDS));
            }
            return limitString;
        }

        private void persistPeers() {
            StringBuilder s = new StringBuilder();
            for (Object peer : peers) {
                if (s.length() > 0) {
                    s.append(Persist.PERSISTED_STRING_SEPARATOR);
                }
                s.append(Persist.persist(peer));
            }
            peerStorage.set(s.toString());
        }

        private String namespaceOf(T item) {
            return item.getClass().getName();
        }

        private String nameOf(T item) {
            String name;
            if (item instanceof Enum<?>) {
                name = ((Enum<?>) item).name();
            } else {
                name = item.toString();
            }
            return name + ".state";
        }

        @Override
        public <S extends Object> State.Options apply(S... peer) {
            applyInternal(peer);
            return this;
        }

        protected <P extends Object> StateImpl<?> applyInternal(P... peer) {
            if (!applied()) {
                setTemporary();
            }
            for (P p : peer) {
                if (!peers.contains(p)) {
                    peers.add(p);
                    Object[] items = new Object[] { item };
                    StateImpl<?> state = (StateImpl<?>) state(p);
                    state.applyInternal(items);
                }
            }
            return this;
        }

        private void setTemporary() {
            over(TEMPORARY, TimeUnit.SECONDS);
        }

        @Override
        public Set<Object> peers() {
            return Collections.unmodifiableSet(peers);
        }

        @Override
        public boolean is(Object object) {
            return peers.contains(object);
        }

        @Override
        public State.Persistence over(long limit, TimeUnit unit) {
            return over(teaseLib.new DurationImpl(limit, unit));
        }

        @Override
        public Persistence over(Duration duration) {
            this.duration = duration;
            return this;
        }

        @Override
        public Duration duration() {
            return duration;
        }

        @Override
        public State remember() {
            rememberMe();
            for (Object s : peers) {
                StateImpl<?> peer = (StateImpl<?>) state(s);
                peer.rememberMe();
            }
            return this;
        }

        private void rememberMe() {
            persistDuration();
            persistPeers();
        }

        @Override
        public boolean applied() {
            return duration.limit(TimeUnit.SECONDS) > REMOVED;
        }

        @Override
        public boolean expired() {
            if (duration.limit(TimeUnit.SECONDS) > TEMPORARY) {
                return isExpired();
            } else {
                for (Object peer : peers) {
                    StateImpl<?> peerState = (StateImpl<?>) state(peer);
                    if (!peerState.isExpired()) {
                        return false;
                    }
                }
                return true;
            }
        }

        boolean isExpired() {
            return duration.expired();
        }

        @Override
        public StateImpl<T> remove() {
            Object[] copyOfPeers = new Object[peers.size()];
            for (Object peer : peers.toArray(copyOfPeers)) {
                state(peer).remove(item);
            }
            peers.clear();
            setRemoved();
            durationStorage.clear();
            peerStorage.clear();
            return this;
        }

        @Override
        public <P extends Object> State remove(P peer) {
            if (peers.contains(peer)) {
                peers.remove(peer);
                state(peer).remove(item);
            }
            if (peers.isEmpty()) {
                setRemoved();
                if (durationStorage.available()) {
                    durationStorage.clear();
                    peerStorage.clear();
                }
            } else if (durationStorage.available()) {
                persistDuration();
                persistPeers();
            }
            return this;
        }

        private void setRemoved() {
            over(REMOVED, TimeUnit.SECONDS);
        }

        @Override
        public String toString() {
            long limit = duration.limit(TimeUnit.SECONDS);
            return nameOf(item) + " " + duration.start(TimeUnit.SECONDS) + (limit > 0 ? "+" : " ")
                    + limit2String(limit) + " " + peers;
        }
    }

    /**
     * Return the state of an enumeration member
     * 
     * @param item
     *            The enumeration member to return the state for
     * @return The item state.
     */
    @SuppressWarnings("unchecked")
    public <T extends Object> State state(T item) {
        return state((Class<T>) item.getClass()).get(item);
    }

    /**
     * Return the state for all or a subset of members of an enumeration.
     * 
     * @param values
     *            The values to retrieve the state for. This should be
     *            {@code Enum.values()}, as the state will only contain the
     *            state items for the listed values.
     * @return The state of all members in {@code values}.
     */
    @SuppressWarnings("unchecked")
    <T extends Object> StateMap<T> state(T[] values) {
        return state((Class<T>) values[0].getClass());
    }

    /**
     * Return the state for all members of an enumeration.
     * 
     * @param enumClass
     *            The class of the enumeration.
     * @return The state of all members of the enumeration.
     */
    @SuppressWarnings("unchecked")
    private <T extends Object> StateMap<T> state(Class<T> enumClass) {
        final StateMap<T> stateMap;
        if (stateMaps.containsKey(enumClass)) {
            stateMap = (StateMap<T>) stateMaps.get(enumClass);
        } else {
            stateMap = new StateMap<T>();
            stateMaps.put(enumClass, stateMap);
        }
        return stateMap;
    }
}
