package teaselib.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import teaselib.Duration;
import teaselib.core.TeaseLib.PersistentString;
import teaselib.core.util.Persist;

public class EnumStateMaps {

    final Map<Class<?>, EnumStateMap<? extends Enum<?>>> stateMaps = new HashMap<Class<?>, EnumStateMap<? extends Enum<?>>>();
    final TeaseLib teaseLib;

    public EnumStateMaps(TeaseLib teaseLib) {
        this.teaseLib = teaseLib;
    }

    void clear() {
        stateMaps.clear();
    }

    interface State {
        public static final long REMOVED = -1;
        public static final long TEMPORARY = 0;

        public static final long INDEFINITELY = Long.MAX_VALUE;

        State.Options apply();

        <S extends Enum<?>> State.Options apply(S... reason);

        boolean applied();

        boolean expired();

        <S extends Enum<?>> State remove();

        <S extends Enum<?>> State remove(S reason);

        interface Options extends State.Persistence {
            Persistence upTo(long duration, TimeUnit unit);
        }

        interface Persistence {
            State remember();
        }
    }

    public class EnumStateMap<T extends Enum<?>> {
        final Map<T, State> states = new HashMap<T, State>();

        public State get(T item) {
            if (states.containsKey(item)) {
                return states.get(item);
            } else {
                State state = new EnumState<T>(item);
                states.put(item, state);
                return state;
            }
        }

        void clear() {
            states.clear();
        }
    }

    public class EnumState<T extends Enum<?>> implements State, State.Options {
        private static final String TEMPORARY_KEYWORD = "TEMPORARY";
        private static final String REMOVED_KEYWORD = "REMOVED";
        private static final String INDEFINITELY_KEYWORD = "INDEFINITELY";

        private final T item;
        private final PersistentString durationStorage;
        private final PersistentString peerStorage;

        private Duration duration = teaseLib.new DurationImpl(REMOVED,
                TimeUnit.SECONDS);
        private final Set<Enum<?>> peers = new HashSet<Enum<?>>();

        public EnumState(T item) {
            super();
            this.item = item;
            this.durationStorage = teaseLib.new PersistentString(
                    TeaseLib.DefaultDomain, namespaceOf(item),
                    nameOf(item) + "." + "duration");
            this.peerStorage = teaseLib.new PersistentString(
                    TeaseLib.DefaultDomain, namespaceOf(item),
                    nameOf(item) + "." + "peers");
            restoreDuration();
            restorePeers();
        }

        private void restoreDuration() {
            if (durationStorage.available()) {
                String[] argv = durationStorage.value().split(" ");
                long start = Long.parseLong(argv[0]);
                long limit = string2limit(argv[1]);
                this.duration = teaseLib.new DurationImpl(start, limit,
                        TimeUnit.SECONDS);
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
                    peers.add((Enum<?>) Persist.from(serializedPeer));
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
            for (Enum<?> peer : peers) {
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
            return item.name() + ".state";
        }

        @Override
        public State.Options apply() {
            remove();
            return this;
        }

        @Override
        public <S extends Enum<?>> State.Options apply(S... peer) {
            applyInternal(peer);
            return this;
        }

        protected <P extends Enum<?>> EnumState<?> applyInternal(P... peer) {
            if (!applied()) {
                setDuration(TEMPORARY, TimeUnit.SECONDS);
            }
            for (P p : peer) {
                if (!peers.contains(p)) {
                    peers.add(p);
                    Enum<?>[] items = new Enum<?>[] { item };
                    EnumState<?> state = (EnumState<?>) state(p);
                    state.applyInternal(items);
                }
            }
            return this;
        }

        @Override
        public State.Persistence upTo(long limit, TimeUnit unit) {
            setDuration(limit, unit);
            return this;
        }

        State setDuration(long limit, TimeUnit unit) {
            this.duration = teaseLib.new DurationImpl(limit, unit);
            return this;
        }

        @Override
        public State remember() {
            rememberMe();
            for (Enum<?> s : peers) {
                EnumState<?> peer = (EnumState<?>) state(s);
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
            return !peers.isEmpty();
        }

        @Override
        public boolean expired() {
            if (duration.limit(TimeUnit.SECONDS) > TEMPORARY) {
                return isExpired();
            } else {
                for (Enum<?> peer : peers) {
                    EnumState<?> peerState = (EnumState<?>) state(peer);
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
        public EnumState<T> remove() {
            Enum<?>[] copyOfReasons = new Enum<?>[peers.size()];
            for (Enum<?> peer : peers.toArray(copyOfReasons)) {
                state(peer).remove(item);
            }
            peers.clear();
            setDuration(REMOVED, TimeUnit.SECONDS);
            durationStorage.clear();
            peerStorage.clear();
            return this;
        }

        @Override
        public <P extends Enum<?>> State remove(P peer) {
            if (peers.contains(peer)) {
                peers.remove(peer);
                state(peer).remove(item);
            }
            if (peers.isEmpty()) {
                setDuration(REMOVED, TimeUnit.SECONDS);
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

        @Override
        public String toString() {
            long limit = duration.limit(TimeUnit.SECONDS);
            return item.name() + " " + duration.start(TimeUnit.SECONDS)
                    + (limit > 0 ? "+" : " ") + limit2String(limit) + " "
                    + peers;
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
    public <T extends Enum<?>> State state(T item) {
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
    <T extends Enum<?>> EnumStateMap<T> state(T[] values) {
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
    private <T extends Enum<?>> EnumStateMap<T> state(Class<T> enumClass) {
        final EnumStateMap<T> stateMap;
        if (stateMaps.containsKey(enumClass)) {
            stateMap = (EnumStateMap<T>) stateMaps.get(enumClass);
        } else {
            stateMap = new EnumStateMap<T>();
            stateMaps.put(enumClass, stateMap);
        }
        return stateMap;
    }
}
