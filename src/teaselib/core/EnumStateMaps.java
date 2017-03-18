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

    private final Map<Class<?>, EnumStateMap<? extends Enum<?>>> stateMaps = new HashMap<Class<?>, EnumStateMap<? extends Enum<?>>>();
    protected final TeaseLib teaseLib;

    public EnumStateMaps(TeaseLib teaseLib) {
        this.teaseLib = teaseLib;
    }

    interface State {
        public static final int REMOVEED = -1;

        State.Options apply();

        <S extends Enum<?>> State.Options apply(S... reason);

        boolean applied();

        boolean expired();
        // TeaseLib.Duration duration();

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
    }

    public class EnumState<T extends Enum<?>> implements State, State.Options {
        private final T item;
        private final PersistentString durationStorage;
        private final PersistentString peersStorage;

        private Duration duration = teaseLib.new DurationImpl(REMOVEED,
                TimeUnit.SECONDS);
        private final Set<Enum<?>> reasons = new HashSet<Enum<?>>();

        public EnumState(T item) {
            super();
            this.item = item;
            this.durationStorage = teaseLib.new PersistentString(
                    TeaseLib.DefaultDomain, namespaceOf(item),
                    nameOf(item) + "." + "duration");
            this.peersStorage = teaseLib.new PersistentString(
                    TeaseLib.DefaultDomain, namespaceOf(item),
                    nameOf(item) + "." + "peers");
            restoreDuration();
            restoreReasons();
        }

        private void restoreDuration() {
            if (durationStorage.available()) {
                String[] argv = durationStorage.value().split(" ");
                long start = Long.parseLong(argv[0]);
                long limit = Long.parseLong(argv[1]);
                this.duration = teaseLib.new DurationImpl(start, limit,
                        TimeUnit.SECONDS);
            }
        }

        private void restoreReasons() {
            if (peersStorage.available()) {
                String[] serializedPeers = peersStorage.value()
                        .split(Persist.PERSISTED_STRING_SEPARATOR);
                for (String serializedPeer : serializedPeers) {
                    reasons.add((Enum<?>) Persist.from(serializedPeer));
                }
            }
        }

        private void persistDuration() {
            durationStorage.set(duration.start(TimeUnit.SECONDS) + " "
                    + duration.limit(TimeUnit.SECONDS));

        }

        private void persistPeers() {
            StringBuilder s = new StringBuilder();
            for (Enum<?> reason : reasons) {
                if (s.length() > 0) {
                    s.append(Persist.PERSISTED_STRING_SEPARATOR);
                    s.append(Persist.persist(reason));
                }
            }
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

        // protected <S extends Enum<?>> State.Options apply(S reason) {
        // if (!reasons.contains(reason)) {
        // reasons.add(reason);
        // state(reason).apply(item);
        // }
        // return this;
        // }

        @Override
        public <S extends Enum<?>> State.Options apply(S... reason) {
            applyInternal(reason);
            return this;
        }

        protected <S extends Enum<?>> EnumState<?> applyInternal(S... reason) {
            for (S s : reason) {
                if (!reasons.contains(s)) {
                    reasons.add(s);
                    Enum<?>[] items = new Enum<?>[] { item };
                    EnumState<?> state = (EnumState<?>) state(s);
                    state.applyInternal(items);
                }
            }
            return this;
        }

        @Override
        public State.Persistence upTo(long limit, TimeUnit unit) {
            setDuration(limit, unit);
            for (Enum<?> reason : reasons) {
                EnumState<?> peer = (EnumState<?>) state(reason);
                peer.setDuration(limit, unit);
            }
            return this;
        }

        State setDuration(long limit, TimeUnit unit) {
            this.duration = teaseLib.new DurationImpl(limit, unit);
            return this;
        }

        @Override
        public State remember() {
            rememberInternal();
            for (Enum<?> s : reasons) {
                EnumState<?> peer = (EnumState<?>) state(s);
                peer.rememberInternal();
            }
            return this;
        }

        protected void addPeersDeep(Set<Enum<?>> deepPeers) {
            for (Enum<?> reason : reasons) {
                boolean contains = deepPeers.contains(reason);
                if (!contains) {
                    deepPeers.add(reason);
                    EnumState<?> peer = (EnumState<?>) state(reason);
                    peer.addPeersDeep(deepPeers);
                }
            }
        }

        private void rememberInternal() {
            persistDuration();
            persistPeers();
        }

        @Override
        public boolean applied() {
            return !reasons.isEmpty();
        }

        @Override
        public boolean expired() {
            return duration.expired();
        }

        @Override
        public EnumState<T> remove() {
            Enum<?>[] copyOfReasons = new Enum<?>[reasons.size()];
            for (Enum<?> reason : reasons.toArray(copyOfReasons)) {
                state(reason).remove(item);
            }
            reasons.clear();
            setDuration(REMOVEED, TimeUnit.SECONDS);
            return this;
        }

        @Override
        public <S extends Enum<?>> State remove(S reason) {
            if (reasons.contains(reason)) {
                reasons.remove(reason);
                state(reason).remove(item);
            }
            if (reasons.isEmpty()) {
                setDuration(REMOVEED, TimeUnit.SECONDS);
            }
            return this;
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
