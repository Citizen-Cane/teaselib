package teaselib.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import teaselib.Duration;
import teaselib.State;
import teaselib.core.TeaseLib.PersistentString;
import teaselib.core.devices.ActionState;
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
    public final String domain;
    public final Object item;

    private final Set<Object> peers = new HashSet<>();
    private final Set<Object> attributes = new HashSet<>();

    private final PersistentString durationStorage;
    private final PersistentString peerStorage;
    private final PersistentString attributeStorage;

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

    public StateImpl(TeaseLib teaseLib, String domain, Object item) {
        this(teaseLib.stateMaps, domain, item);
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

    private static long string2limit(String limitString) {
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
            Persist.Storage storage = new Persist.Storage(Arrays.asList(Persist.persistedValue(persistedPeer)));
            ItemImpl peer = ItemImpl.restore(stateMaps.teaseLib, domain, storage);
            addPeerThatHasBeenPersistedWithMe(peer, QualifiedItem.of(peer.item));
        } else {
            addAppliedOrPersistedPeer(Persist.<Object> from(persistedPeer));
        }
    }

    private void addAppliedOrPersistedPeer(Object peer) {
        addPeerThatHasBeenPersistedWithMe(peer, QualifiedItem.of(peer));
    }

    private void addPeerThatHasBeenPersistedWithMe(Object peer, QualifiedItem qualifiedPeer) {
        if (isCached(qualifiedPeer)) {
            if (state(peer).applied()) {
                peers.add(peer);
            }
        } else if (persistentDuration(domain, peer).available()) {
            peers.add(peer);
        }
    }

    private boolean isCached(QualifiedItem qualifiedPeer) {
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
    public State.Options applyTo(Object... attributes) {
        applyInternal(attributes);
        return this;
    }

    protected State applyInternal(Object... attributes) {
        if (attributes.length == 1 && attributes[0] instanceof List<?>) {
            throw new IllegalArgumentException();
        }

        if (!applied() && !stateAppliesBeforehand(attributes)) {
            setTemporary();
        }

        for (Object attribute : attributes) {
            if (!peers.contains(attribute)) {
                peers.add(attribute);
                StateImpl state = state(attribute);
                state.applyInternal(item);
            }
        }
        return this;
    }

    private boolean stateAppliesBeforehand(Object... attributes) {
        return attributes.length == 1 && state(attributes[0]) instanceof ActionState;
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

        if (!allItemInstancesFoundInPeers(attributes)) {
            return false;
        }

        return StateMaps.hasAllAttributes(attributesAndPeers(), attributes);
    }

    private boolean allItemInstancesFoundInPeers(Object... attributes) {
        List<Object> items = Arrays.stream(attributes).filter(attribute -> attribute instanceof Item)
                .collect(Collectors.toList());
        return items.stream().filter(attribute -> peers.stream().anyMatch(attribute::equals)).count() == items.size();
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
    public State over(long limit, TimeUnit unit) {
        return over(new DurationImpl(this.stateMaps.teaseLib, limit, unit));
    }

    @Override
    public State over(Duration duration) {
        this.duration = duration;
        if (duration.limit(TimeUnit.MILLISECONDS) != TEMPORARY) {
            remember();
        }
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

    private void remember() {
        updatePersistence();
        for (Object s : peers) {
            StateImpl peer = state(s);
            peer.updatePersistence();
        }
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
    public State remove() {
        if (!peers.isEmpty()) {
            Object[] copyOfPeers = new Object[peers.size()];
            for (Object peer : peers.toArray(copyOfPeers)) {
                state(peer).removeFrom(item);
            }
            peers.clear();
        }

        attributes.clear();
        setRemoved();
        if (isPersisted()) {
            removePersistence();
        }
        return this;
    }

    @Override
    public State removeFrom(Object... peers2) {
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
            remove();
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
        result = prime * result + this.stateMaps.hashCode();
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
        if (!this.stateMaps.equals(other.stateMaps))
            return false;
        if (attributeStorage == null) {
            if (other.attributeStorage != null)
                return false;
        } else if (!attributeStorage.equals(other.attributeStorage))
            return false;
        if (!attributes.equals(other.attributes))
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
        if (!peers.equals(other.peers))
            return false;
        return true;
    }

}