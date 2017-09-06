package teaselib.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import teaselib.Duration;
import teaselib.State;
import teaselib.core.TeaseLib.PersistentString;
import teaselib.core.state.AbstractProxy;
import teaselib.core.state.StateProxy;
import teaselib.core.util.Persist;
import teaselib.core.util.QualifiedItem;
import teaselib.util.Item;
import teaselib.util.ItemImpl;

public class StateMaps {
    final TeaseLib teaseLib;

    public interface Attributes {
        void applyAttributes(Object... attributes);
    }

    class StateMapCache extends HashMap<String, StateMap> {
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

    public class StateMap {
        final String domain;
        final Map<Object, State> states = new HashMap<Object, State>();

        public StateMap(String domain) {
            this.domain = domain;
        }

        public State get(Object item) {
            if (states.containsKey(item)) {
                return states.get(item);
            } else {
                return null;
            }
        }

        public void put(Object item, State state) {
            states.put(item, state);
        }

        void clear() {
            states.clear();
        }

        public boolean contains(Object item) {
            return states.containsKey(item);
        }
    }

    static String nameOfState(Object item) {
        String name = QualifiedItem.nameOf(item);
        return name + ".state";
    }

    public class StateImpl implements State, State.Options, Attributes {
        private static final String TEMPORARY_KEYWORD = "TEMPORARY";
        private static final String REMOVED_KEYWORD = "REMOVED";
        private static final String INDEFINITELY_KEYWORD = "INDEFINITELY";

        protected final String domain;
        private final Object item;
        private final PersistentString durationStorage;
        private final PersistentString peerStorage;
        private final PersistentString attributeStorage;

        private Duration duration = teaseLib.new DurationImpl(0, REMOVED, TimeUnit.SECONDS);
        private final Set<Object> peers = new HashSet<Object>();
        private final Set<Object> attributes = new HashSet<Object>();

        protected StateImpl state(Object item) {
            if (item instanceof AbstractProxy<?>) {
                return state(((AbstractProxy<?>) item).state);
            } else if (item instanceof ItemImpl) {
                return state(((ItemImpl) item).item);
            } else if (item instanceof StateImpl) {
                return (StateImpl) item;
            } else {
                return (StateImpl) StateMaps.this.state(domain, item);
            }
        }

        public StateImpl(String domain, Object item) {
            super();

            if ((item instanceof State) && !(item instanceof Item)) {
                throw new IllegalArgumentException(item.toString());
            }

            this.domain = domain;
            this.item = item;
            this.durationStorage = persistentDuration(domain, item);
            this.peerStorage = persistentPeers(domain, item);
            this.attributeStorage = persistentAttributes(domain, item);
            restoreDuration();
            restoreAttributes();
            restorePeers();
        }

        private TeaseLib.PersistentString persistentDuration(String domain, Object item) {
            return teaseLib.new PersistentString(domain, QualifiedItem.namespaceOf(item),
                    nameOfState(item) + "." + "duration");
        }

        private PersistentString persistentPeers(String domain, Object item) {
            return teaseLib.new PersistentString(domain, QualifiedItem.namespaceOf(item),
                    nameOfState(item) + "." + "peers");
        }

        private PersistentString persistentAttributes(String domain, Object item) {
            return teaseLib.new PersistentString(domain, QualifiedItem.namespaceOf(item),
                    nameOfState(item) + "." + "attributes");
        }

        private void restoreDuration() {
            if (isPersisted()) {
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
                String[] serializedPeers = peerStorage.value().split(Persist.PERSISTED_STRING_SEPARATOR);
                for (String serializedPeer : serializedPeers) {
                    restorePersistedPeer(serializedPeer);
                }

                if (peers.isEmpty()) {
                    remove();
                }
            }
        }

        private void restorePersistedPeer(String persistedPeer) {
            if (Persist.className(persistedPeer).equals(ItemImpl.class.getName())) {
                String guid = Persist.persistedValue(persistedPeer);
                ItemImpl peer = (ItemImpl) teaseLib.item(domain, guid);

                addPeerThatHasBeenPersistedWithMe(peer, QualifiedItem.of(peer.item));
            } else {
                addAppliedOrPersistedPeer(Persist.<Object> from(persistedPeer));
            }
        }

        private void addAppliedOrPersistedPeer(Object peer) {
            addPeerThatHasBeenPersistedWithMe(peer, QualifiedItem.of(peer));
        }

        private void addPeerThatHasBeenPersistedWithMe(Object peer, QualifiedItem<?> qualifiedPeer) {
            if (isCached(qualifiedPeer)) {
                if (state(peer).applied()) {
                    peers.add(peer);
                }
            } else if (persistentDuration(domain, peer).available()) {
                peers.add(peer);
            }
        }

        private boolean isCached(QualifiedItem<?> qualifiedPeer) {
            return stateMap(domain, qualifiedPeer.namespace().toLowerCase())
                    .contains(qualifiedPeer.name().toLowerCase());
        }

        private void restoreAttributes() {
            if (attributeStorage.available()) {
                String[] serializedAttributes = attributeStorage.value().split(Persist.PERSISTED_STRING_SEPARATOR);
                for (String serializedAttribute : serializedAttributes) {
                    attributes.add(Persist.from(serializedAttribute));
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
            if (peers.isEmpty()) {
                peerStorage.clear();
            } else {
                StringBuilder s = new StringBuilder();
                for (Object peer : peers) {
                    if (s.length() > 0) {
                        s.append(Persist.PERSISTED_STRING_SEPARATOR);
                    }
                    s.append(Persist.persist(peer));
                }
                peerStorage.set(s.toString());
            }
        }

        private void persistAttributes() {
            if (attributes.isEmpty()) {
                attributeStorage.clear();
            } else {
                StringBuilder persisted = new StringBuilder();
                for (Object attribute : attributes) {
                    if (persisted.length() > 0) {
                        persisted.append(Persist.PERSISTED_STRING_SEPARATOR);
                    }
                    persisted.append(Persist.persist(attribute));
                }
                attributeStorage.set(persisted.toString());
            }
        }

        @Override
        public Options apply() {
            return applyTo(new Object[0]);
        }

        @Override
        public <A extends Object> State.Options applyTo(A... attributes) {
            applyInternal(attributes);
            return this;
        }

        protected <A extends Object> State applyInternal(A... attributes) {
            if (attributes.length == 1 && attributes[0] instanceof List<?>) {
                throw new IllegalArgumentException();
            }

            if (!applied()) {
                setTemporary();
            }
            for (A attribute : attributes) {
                if (!peers.contains(attribute)) {
                    peers.add(attribute);
                    Object[] items = new Object[] { item };
                    StateImpl state = state(attribute);
                    state.applyInternal(items);
                }
            }
            return this;
        }

        private void setTemporary() {
            over(TEMPORARY, TimeUnit.SECONDS);
        }

        public Set<Object> peers() {
            return Collections.unmodifiableSet(peers);
        }

        @Override
        public void applyAttributes(Object... attributes) {
            if (attributes.length == 1 && attributes[0] instanceof List<?>) {
                throw new IllegalArgumentException();
            }

            this.attributes.addAll(Arrays.asList(attributes));
        }

        @Override
        public boolean is(Object... attributes) {
            if (attributes.length == 1 && attributes[0] instanceof List<?>) {
                throw new IllegalArgumentException();
            }

            return hasAllAttributes(attributesAndPeers(), attributes);
        }

        private Set<Object> attributesAndPeers() {
            Set<Object> all = new HashSet<Object>();
            all.addAll(myAttributesAndPeers());
            all.addAll(attributesOfDirectPeers());

            for (Object item : myAttributesAndPeers()) {
                if (item instanceof ItemImpl) {
                    all.addAll(((ItemImpl) item).attributesAndPeers());
                }
            }
            return all;
        }

        private Set<Object> myAttributesAndPeers() {
            Set<Object> myAttributesAndPeers = new HashSet<Object>();
            myAttributesAndPeers.addAll(this.peers);
            myAttributesAndPeers.addAll(this.attributes);
            return myAttributesAndPeers;
        }

        private Set<Object> attributesOfDirectPeers() {
            Set<Object> attributesOfDirectPeers = new HashSet<Object>();
            for (Object peer : this.peers) {
                StateImpl peerState = state(peer);
                attributesOfDirectPeers.addAll(peerState.attributes);
            }
            return attributesOfDirectPeers;
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
            Duration maximum = this.duration;
            for (Object s : peers) {
                StateImpl peer = state(s);
                if (peer.duration.remaining(TimeUnit.SECONDS) > maximum.remaining(TimeUnit.SECONDS)) {
                    maximum = peer.duration;
                }
            }
            return maximum;
        }

        @Override
        public State remember() {
            updatePersistence();
            for (Object s : peers) {
                StateImpl peer = state(s);
                peer.updatePersistence();
            }
            return this;
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
                    StateImpl peerState = state(peer);
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
        public Persistence remove() {
            Object[] copyOfPeers = new Object[peers.size()];
            for (Object peer : peers.toArray(copyOfPeers)) {
                state(peer).removeFrom(item);
            }

            peers.clear();
            attributes.clear();
            setRemoved();
            removePersistence();
            return this;
        }

        @Override
        public <S extends Object> Persistence removeFrom(S... peers2) {
            if (peers2.length == 0) {
                throw new IllegalArgumentException("removeFrom requires at least one peer");
            }

            for (Object peer : peers2) {
                if (peer instanceof List<?> || peer instanceof Object[]) {
                    throw new IllegalArgumentException();
                }

                if (peers.contains(peer)) {
                    peers.remove(peer);
                    if (!(peer instanceof ItemImpl)) {
                        removeRepresentingItems(peer);
                    }
                    state(peer).removeFrom(item);
                }

                if (allPeersAreTemporary() && isPersisted()) {
                    removePersistence();
                }
            }

            if (peers.isEmpty()) {
                attributes.clear();
                setRemoved();
                if (isPersisted()) {
                    removePersistence();
                }
            } else if (isPersisted()) {
                updatePersistence();
            }
            return this;
        }

        private void removeRepresentingItems(Object rawItem) {
            for (Object object : new HashSet<Object>(peers)) {
                if (object instanceof ItemImpl) {
                    ItemImpl itemImpl = (ItemImpl) object;
                    if (itemImpl.item == rawItem) {
                        peers.remove(itemImpl);
                    }
                }
            }
        }

        private void updatePersistence() {
            persistDuration();
            persistPeers();
            persistAttributes();
        }

        private boolean allPeersAreTemporary() {
            for (Object peer : peers) {
                if (state(peer).isPersisted()) {
                    return false;
                }
            }
            return true;
        }

        private boolean isPersisted() {
            return durationStorage.available();
        }

        private void removePersistence() {
            durationStorage.clear();
            peerStorage.clear();
            attributeStorage.clear();
        }

        private void setRemoved() {
            over(REMOVED, TimeUnit.SECONDS);
        }

        @Override
        public String toString() {
            String name = domain + " " + nameOfState(item);

            Date date = new Date(duration.start(TimeUnit.MILLISECONDS));

            long limit = duration.limit(TimeUnit.SECONDS);
            String timespan = (limit > 0 ? "+" : " ") + limit2String(limit);

            return name + " " + date + timespan + " " + toStringWithoutRecursion(peers);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((attributeStorage == null) ? 0 : attributeStorage.hashCode());
            result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
            result = prime * result + ((domain == null) ? 0 : domain.hashCode());
            result = prime * result + ((duration == null) ? 0 : duration.hashCode());
            result = prime * result + ((durationStorage == null) ? 0 : durationStorage.hashCode());
            result = prime * result + ((item == null) ? 0 : item.hashCode());
            result = prime * result + ((peerStorage == null) ? 0 : peerStorage.hashCode());
            result = prime * result + ((peers == null) ? 0 : peers.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof AbstractProxy<?>) {
                return equals(((AbstractProxy<?>) obj).state);
            } else {
                return generatedEqualsMethod(obj);
            }
        }

        private boolean generatedEqualsMethod(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            StateImpl other = (StateImpl) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (attributeStorage == null) {
                if (other.attributeStorage != null)
                    return false;
            } else if (!attributeStorage.equals(other.attributeStorage))
                return false;
            if (attributes == null) {
                if (other.attributes != null)
                    return false;
            } else if (!attributes.equals(other.attributes))
                return false;
            if (domain == null) {
                if (other.domain != null)
                    return false;
            } else if (!domain.equals(other.domain))
                return false;
            if (duration == null) {
                if (other.duration != null)
                    return false;
            } else if (!duration.equals(other.duration))
                return false;
            if (durationStorage == null) {
                if (other.durationStorage != null)
                    return false;
            } else if (!durationStorage.equals(other.durationStorage))
                return false;
            if (item == null) {
                if (other.item != null)
                    return false;
            } else if (!item.equals(other.item))
                return false;
            if (peerStorage == null) {
                if (other.peerStorage != null)
                    return false;
            } else if (!peerStorage.equals(other.peerStorage))
                return false;
            if (peers == null) {
                if (other.peers != null)
                    return false;
            } else if (!peers.equals(other.peers))
                return false;
            return true;
        }

        private StateMaps getOuterType() {
            return StateMaps.this;
        }

    }

    private static String toStringWithoutRecursion(Set<Object> peers) {
        StringBuilder toString = new StringBuilder();
        for (Object object : peers) {
            if (toString.length() == 0) {
                toString.append("[");
            } else {
                toString.append(", ");
            }
            if (object instanceof StateImpl) {
                StateImpl state = (StateImpl) object;
                toString.append(state.item.toString());
            }
        }
        toString.append("]");
        return toString.toString();
    }

    /**
     * Return the state of an enumeration member
     * 
     * @param item
     *            The enumeration member to return the state for
     * @return The item state.
     */

    public State state(String domain, Object item) {
        return state(domain, QualifiedItem.of(item));
    }

    private State state(String domain, QualifiedItem<?> item) {
        StateMap stateMap = stateMap(domain, item);
        State state = stateMap.get(item.toString().toLowerCase());
        if (state == null) {
            state = new StateImpl(domain, item.value);
            stateMap.put(item.toString().toLowerCase(), state);
        }
        return state;
    }

    public static boolean hasAllAttributes(Set<Object> mine, Object[] others) {
        attributeLoop: for (Object value : others) {
            QualifiedItem<?> item = QualifiedItem.of(stripState(value));
            for (Object attribute : mine) {
                if (item.equals(stripState(attribute))) {
                    continue attributeLoop;
                }
            }
            return false;
        }
        return true;
    }

    private static Object stripState(Object value) {
        if (value instanceof StateImpl) {
            return stripState((StateImpl) value);
        } else if (value instanceof StateProxy) {
            return stripState(((StateProxy) value).state);
        } else {
            return value;
        }
    }

    private static Object stripState(StateImpl state) {
        return state.item;
    }

    private StateMap stateMap(String domain, QualifiedItem<?> item) {
        return stateMap(domain, item.namespace().toLowerCase());
    }

    private StateMap stateMap(String domain, String namespaceKey) {
        StateMapCache domainCache = getDomainCache(domain.toLowerCase());
        final StateMap stateMap;
        if (domainCache.containsKey(namespaceKey)) {
            stateMap = domainCache.get(namespaceKey);
        } else {
            stateMap = new StateMap(domain);
            domainCache.put(namespaceKey, stateMap);
        }
        return stateMap;
    }

    private StateMapCache getDomainCache(String domainKey) {
        final StateMapCache domainCache;
        if (cache.containsKey(domainKey)) {
            domainCache = cache.get(domainKey);
        } else {
            domainCache = new StateMapCache();
            cache.put(domainKey, domainCache);
        }
        return domainCache;
    }
}
