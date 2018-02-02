package teaselib.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import teaselib.Duration;
import teaselib.State;
import teaselib.core.TeaseLib.PersistentString;
import teaselib.core.state.AbstractProxy;
import teaselib.core.util.Persist;
import teaselib.core.util.QualifiedItem;
import teaselib.util.Item;
import teaselib.util.ItemImpl;

public class StateImpl implements State, State.Options, StateMaps.Attributes {

    private static final String TEMPORARY_KEYWORD = "TEMPORARY";
    private static final String REMOVED_KEYWORD = "REMOVED";
    private static final String INDEFINITELY_KEYWORD = "INDEFINITELY";

    private final StateMaps stateMaps;
    protected final String domain;
    final Object item;
    private final PersistentString durationStorage;
    private final PersistentString peerStorage;
    private final PersistentString attributeStorage;

    private final Set<Object> peers = new HashSet<>();
    private final Set<Object> attributes = new HashSet<>();

    private Duration duration;

    protected StateImpl state(Object item) {
        if (item instanceof AbstractProxy<?>) {
            return state(((AbstractProxy<?>) item).state);
        } else if (item instanceof ItemImpl) {
            return state(((ItemImpl) item).item);
        } else if (item instanceof StateImpl) {
            return (StateImpl) item;
        } else {
            return (StateImpl) this.stateMaps.state(domain, item);
        }
    }

    public StateImpl(StateMaps stateMaps, String domain, Object item) {
        super();
        this.stateMaps = stateMaps;

        if ((item instanceof State) && !(item instanceof Item)) {
            throw new IllegalArgumentException(item.toString());
        }

        this.domain = domain;
        this.item = item;
        this.durationStorage = persistentDuration(domain, item);
        this.peerStorage = persistentPeers(domain, item);
        this.attributeStorage = persistentAttributes(domain, item);

        this.duration = new DurationImpl(this.stateMaps.teaseLib, 0, REMOVED, TimeUnit.SECONDS);

        restoreDuration();
        restoreAttributes();
        restorePeers();
    }

    private TeaseLib.PersistentString persistentDuration(String domain, Object item) {
        return persistentString(domain, item, "duration");
    }

    private PersistentString persistentPeers(String domain, Object item) {
        return persistentString(domain, item, "peers");
    }

    private PersistentString persistentAttributes(String domain, Object item) {
        return persistentString(domain, item, "attributes");
    }

    private PersistentString persistentString(String domain, Object item, String name) {
        return this.stateMaps.teaseLib.new PersistentString(domain, QualifiedItem.namespaceOf(item),
                QualifiedItem.nameOf(item) + ".state" + "." + name);
    }

    private void restoreDuration() {
        if (isPersisted()) {
            String[] argv = durationStorage.value().split(" ");
            long start = Long.parseLong(argv[0]);
            long limit = string2limit(argv[1]);
            this.duration = new DurationImpl(this.stateMaps.teaseLib, start, limit, TimeUnit.SECONDS);
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
            ItemImpl peer = (ItemImpl) this.stateMaps.teaseLib.item(domain, guid);

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
        return this.stateMaps.stateMap(domain, qualifiedPeer.namespace().toLowerCase())
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
        return applyTo();
    }

    @Override
    @SafeVarargs
    public final <A extends Object> State.Options applyTo(A... attributes) {
        applyInternal(attributes);
        return this;
    }

    @SafeVarargs
    protected final <A extends Object> State applyInternal(A... attributes) {
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

        return StateMaps.hasAllAttributes(attributesAndPeers(), attributes);
    }

    private Set<Object> attributesAndPeers() {
        Set<Object> all = new HashSet<>();
        all.addAll(myAttributesAndPeers());
        all.addAll(attributesOfDirectPeers());

        for (Object peer : myAttributesAndPeers()) {
            if (peer instanceof ItemImpl) {
                all.addAll(((ItemImpl) peer).attributesAndPeers());
            }
        }
        return all;
    }

    private Set<Object> myAttributesAndPeers() {
        Set<Object> myAttributesAndPeers = new HashSet<>();
        myAttributesAndPeers.addAll(this.peers);
        myAttributesAndPeers.addAll(this.attributes);
        return myAttributesAndPeers;
    }

    private Set<Object> attributesOfDirectPeers() {
        Set<Object> attributesOfDirectPeers = new HashSet<>();
        for (Object peer : this.peers) {
            StateImpl peerState = state(peer);
            attributesOfDirectPeers.addAll(peerState.attributes);
        }
        return attributesOfDirectPeers;
    }

    @Override
    public State.Persistence over(long limit, TimeUnit unit) {
        return over(new DurationImpl(this.stateMaps.teaseLib, limit, unit));
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
    @SafeVarargs
    public final <S extends Object> Persistence removeFrom(S... peers2) {
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
        for (Object object : new HashSet<>(peers)) {
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
        String name = "name=" + (domain.isEmpty() ? "" : domain) + QualifiedItem.nameOf(item);

        Date date = new Date(duration.start(TimeUnit.MILLISECONDS));

        long limit = duration.limit(TimeUnit.SECONDS);
        String timespan = (limit > 0 ? "+" : " ") + limit2String(limit);

        return name + " " + date + timespan + " peers=" + StateMaps.toStringWithoutRecursion(peers);
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
        return this.stateMaps;
    }

}