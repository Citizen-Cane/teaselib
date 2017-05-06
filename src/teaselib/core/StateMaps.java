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
import teaselib.core.util.QualifiedItem;

public class StateMaps {
    final TeaseLib teaseLib;

    class StateMapCache extends HashMap<String, StateMap<? extends Object>> {
        private static final long serialVersionUID = 1L;
    }

    class Domains extends HashMap<String, StateMapCache> {
        private static final long serialVersionUID = 1L;
    }

    final Domains cache = new Domains();

    public StateMaps(TeaseLib teaseLib) {
        this.teaseLib = teaseLib;
    }

    void clear() {
        cache.clear();
    }

    public class StateMap<T extends Object> {
        final String domain;
        final Map<T, State> states = new HashMap<T, State>();

        public StateMap(String domain) {
            this.domain = domain;
        }

        public State get(T item) {
            if (states.containsKey(item)) {
                return states.get(item);
            } else {
                return null;
            }
        }

        public void put(T item, State state) {
            states.put(item, state);
        }

        void clear() {
            states.clear();
        }
    }

    static String nameOfState(Object item) {
        String name = QualifiedItem.nameOf(item);
        return name + ".state";
    }

    public class StateImpl<T extends Object> implements State, State.Options {
        private static final String TEMPORARY_KEYWORD = "TEMPORARY";
        private static final String REMOVED_KEYWORD = "REMOVED";
        private static final String INDEFINITELY_KEYWORD = "INDEFINITELY";

        protected final String domain;
        private final T item;
        private final PersistentString durationStorage;
        private final PersistentString peerStorage;

        private Duration duration = teaseLib.new DurationImpl(0, REMOVED, TimeUnit.SECONDS);
        private final Set<Object> peers = new HashSet<Object>();

        @SuppressWarnings("unchecked")
        protected StateImpl<T> state(Object item) {
            return (StateImpl<T>) StateMaps.this.state(domain, item);
        }

        public StateImpl(String domain, T item) {
            super();
            this.domain = domain;
            this.item = item;
            this.durationStorage = teaseLib.new PersistentString(TeaseLib.DefaultDomain,
                    QualifiedItem.namespaceOf(item), nameOfState(item) + "." + "duration");
            this.peerStorage = teaseLib.new PersistentString(TeaseLib.DefaultDomain,
                    QualifiedItem.namespaceOf(item), nameOfState(item) + "." + "peers");
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

        @Override
        public <S extends Object> State.Options apply(S... peer) {
            applyInternal(peer);
            return this;
        }

        protected <P extends Object> State applyInternal(P... peer) {
            if (!applied()) {
                setTemporary();
            }
            for (P p : peer) {
                if (!peers.contains(p)) {
                    peers.add(p);
                    Object[] items = new Object[] { item };
                    StateImpl<T> state = state(p);
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
                StateImpl<?> peer = state(s);
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
                    StateImpl<T> peerState = state(peer);
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
            return nameOfState(item) + " " + duration.start(TimeUnit.SECONDS)
                    + (limit > 0 ? "+" : " ") + limit2String(limit) + " " + peers;
        }
    }

    /**
     * Return the state of an enumeration member
     * 
     * @param item
     *            The enumeration member to return the state for
     * @return The item state.
     */
    public <T extends Object> State state(String domain, T item) {
        StateMap<Object> stateMap = stateMap(domain, QualifiedItem.namespaceOf(item));
        State state = stateMap.get(item);
        if (state == null) {
            state = new StateImpl<Object>(domain, item);
            stateMap.put(item, state);
        }
        return state;
    }

    @SuppressWarnings("unchecked")
    private <T> StateMap<T> stateMap(String domain, String namespace) {
        StateMapCache domainCache = getDomainCache(domain);
        final StateMap<T> stateMap;
        if (domainCache.containsKey(namespace)) {
            stateMap = (StateMap<T>) domainCache.get(namespace);
        } else {
            stateMap = new StateMap<T>(domain);
            domainCache.put(namespace, stateMap);
        }
        return stateMap;
    }

    private StateMapCache getDomainCache(String domain) {
        final StateMapCache domainCache;
        if (cache.containsKey(domain)) {
            domainCache = cache.get(domain);
        } else {
            domainCache = new StateMapCache();
            cache.put(domain, domainCache);
        }
        return domainCache;
    }
}
