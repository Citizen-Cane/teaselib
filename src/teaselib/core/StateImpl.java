package teaselib.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import teaselib.Duration;
import teaselib.State;
import teaselib.core.TeaseLib.PersistentBoolean;
import teaselib.core.TeaseLib.PersistentString;
import teaselib.core.devices.ActionState;
import teaselib.core.state.AbstractProxy;
import teaselib.core.util.Persist;
import teaselib.core.util.PersistedObject;
import teaselib.core.util.QualifiedItem;
import teaselib.core.util.Storage;
import teaselib.util.Item;
import teaselib.util.ItemGuid;
import teaselib.util.ItemImpl;

public class StateImpl implements State, State.Options, StateMaps.Attributes {
    private static final String TEMPORARY_KEYWORD = "TEMPORARY";
    private static final String INDEFINITELY_KEYWORD = "INDEFINITELY";

    private final StateMaps stateMaps;
    public final String domain;
    public final Object item;

    private final Set<Object> peers = new HashSet<>();
    private final Set<Object> attributes = new HashSet<>();
    private boolean applied = false;

    private final PersistentBoolean appliedStorage;
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

    StateImpl(StateMaps stateMaps, String domain, Object item) {
        this.stateMaps = stateMaps;

        if ((item instanceof State) && !(item instanceof Item)) {
            throw new IllegalArgumentException(item.toString());
        }

        if (item instanceof ItemGuid) {
            throw new IllegalArgumentException("Guids cannot be states: " + item.toString());
        }

        this.domain = domain;
        this.item = item;
        this.appliedStorage = persistentApplied(domain, item);
        this.durationStorage = persistentDuration(domain, item);
        this.peerStorage = persistentPeers(domain, item);
        this.attributeStorage = persistentAttributes(domain, item);

        this.duration = new DurationImpl(this.stateMaps.teaseLib, 0, 0, TimeUnit.SECONDS);

        restoreApplied();
        restoreDuration();
        restoreAttributes();
        restorePeers();
    }

    protected StateImpl(TeaseLib teaseLib, String domain, Object item) {
        this(teaseLib.stateMaps, domain, item);
    }

    private TeaseLib.PersistentBoolean persistentApplied(String domain, Object item) {
        return persistentBoolean(domain, item, "applied");
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

    private PersistentBoolean persistentBoolean(String domain, Object item, String name) {
        return this.stateMaps.teaseLib.new PersistentBoolean(domain, QualifiedItem.namespaceOf(item),
                QualifiedItem.nameOf(item) + ".state" + "." + name);
    }

    private PersistentString persistentString(String domain, Object item, String name) {
        return this.stateMaps.teaseLib.new PersistentString(domain, QualifiedItem.namespaceOf(item),
                QualifiedItem.nameOf(item) + ".state" + "." + name);
    }

    private void restoreApplied() {
        applied = appliedStorage.value();
    }

    private void restoreDuration() {
        if (isPersisted()) {
            // TODO Refactor persistence to duration class
            String[] argv = durationStorage.value().split(" ");
            long start = Long.parseLong(argv[0]);
            long limit = string2limit(argv[1]);
            this.duration = new DurationImpl(this.stateMaps.teaseLib, start, limit, TimeUnit.SECONDS);
        }
    }

    private static long string2limit(String limitString) {
        long limit;
        if (limitString.equals(TEMPORARY_KEYWORD)) {
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
            String persisted = peerStorage.value();
            List<String> presistedPeers = new PersistedObject(ArrayList.class, persisted).toValues();
            for (String persistedPeer : presistedPeers) {
                restorePersistedPeer(persistedPeer);
            }

            if (peers.isEmpty()) {
                remove();
            }
        }
    }

    private void restorePersistedPeer(String persistedPeer) {
        if (PersistedObject.className(persistedPeer).equals(ItemImpl.class.getName())) {
            Storage storage = Storage.from(persistedPeer);
            ItemImpl peer = ItemImpl.restoreFromUserItems(stateMaps.teaseLib, domain, storage);
            addPeerThatHasBeenPersistedWithMe(peer, QualifiedItem.of(peer));
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
        } else if (qualifiedPeer.value() instanceof Item) {
            peers.add(peer);
        }
    }

    private boolean isCached(QualifiedItem qualifiedPeer) {
        return this.stateMaps.stateMap(domain, qualifiedPeer.namespace().toLowerCase())
                .contains(qualifiedPeer.name().toLowerCase());
    }

    private void restoreAttributes() {
        if (attributeStorage.available()) {
            attributes.addAll(Persist.from(ArrayList.class, attributeStorage.value()));
        }
    }

    private void persistApplied() {
        appliedStorage.set(applied);
    }

    private void persistDuration() {
        String startValue = Long.toString(duration.start(TimeUnit.SECONDS));
        long limit = duration.limit(TimeUnit.SECONDS);
        String limitValue = limit2String(limit);
        durationStorage.set(startValue + " " + limitValue);

    }

    private String limit2String(long limit) {
        String limitString;
        if (limit < 0) {
            limitString = Long.toString(duration.limit(TimeUnit.SECONDS));
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
            peerStorage.set(Persist.persistValues(peers));
        }
    }

    private void persistAttributes() {
        if (attributes.isEmpty()) {
            attributeStorage.clear();
        } else {
            attributeStorage.set(Persist.persistValues(attributes));
        }
    }

    @Override
    public Options apply() {
        return applyTo();
    }

    @Override
    public State.Options applyTo(Object... attributes) {
        applyInternal(StateMaps.flatten(attributes));
        return this;
    }

    protected State applyInternal(Object... attributes) {
        if (!applied() && !stateAppliesBeforehand(attributes)) {
            setTemporary();
        }

        for (Object attribute : AbstractProxy.removeProxies(attributes)) {
            if (!peers.contains(attribute)) {
                peers.add(attribute);
                if (!(attribute instanceof ItemGuid)) {
                    StateImpl state = state(attribute);
                    state.applyInternal(item);
                }
            }
        }

        if (!stateAppliesBeforehand(attributes)) {
            setApplied();
        }
        return this;
    }

    private boolean stateAppliesBeforehand(Object... attributes) {
        if (attributes.length == 0) {
            return false;
        } else {
            return Arrays.stream(attributes).filter(ItemGuid::isntItemGuid).map(this::state)
                    .allMatch(ActionState::isActionState);
        }
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

    public Set<Object> getAttributes() {
        return Collections.unmodifiableSet(attributes);
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
        List<Object> instances = Arrays.stream(attributes).filter(attribute -> attribute instanceof Item)
                .map(AbstractProxy::removeProxy).collect(Collectors.toList());
        List<ItemGuid> guids = Arrays.stream(attributes).filter(attribute -> attribute instanceof Item)
                .map(AbstractProxy::removeProxy).map(instance -> ((ItemImpl) instance).guid)
                .collect(Collectors.toList());
        return peers().containsAll(instances) || peers().containsAll(guids);
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
        return peers().stream().filter(ItemGuid::isntItemGuid).map(this::state).map(state -> state.attributes)
                .flatMap(Set::stream).collect(Collectors.toSet());
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
        Optional<Duration> maximum = Stream
                .concat(Stream.of(this.duration),
                        peers.stream().filter(ItemGuid::isntItemGuid).map(this::state).map(state -> state.duration))
                .max((a, b) -> Long.compare(a.remaining(TimeUnit.SECONDS), b.remaining(TimeUnit.SECONDS)));
        if (maximum.isPresent()) {
            return maximum.get();
        } else {
            return duration;
        }
    }

    private void remember() {
        updatePersistence();
        for (Object peer : peers) {
            if (!(peer instanceof ItemGuid)) {
                StateImpl peerState = state(peer);
                peerState.updatePersistence();
            }
        }
    }

    @Override
    public boolean applied() {
        return applied;
    }

    @Override
    public boolean expired() {
        if (duration.limit(TimeUnit.SECONDS) > TEMPORARY) {
            return isExpired();
        } else {
            for (Object peer : peers) {
                if (!(peer instanceof ItemGuid)) {
                    StateImpl peerState = state(peer);
                    if (!peerState.isExpired()) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    boolean isExpired() {
        return duration.expired();
    }

    @Override
    public void remove() {
        if (!peers.isEmpty()) {
            Object[] copyOfPeers = new Object[peers.size()];
            for (Object peer : peers.toArray(copyOfPeers)) {
                if (peer instanceof ItemGuid) {
                    peers.remove(peer);
                } else {
                    state(peer).removeFrom(item);
                }
            }
            peers.clear();
        }

        attributes.clear();
        setRemoved();
        if (isPersisted()) {
            removePersistence();
        }
    }

    @Override
    public void removeFrom(Object... peers2) {
        if (peers2.length == 0) {
            throw new IllegalArgumentException("removeFrom requires at least one peer");
        }

        for (Object peer : AbstractProxy.removeProxies(peers2)) {
            if (peer instanceof Collection<?> || peer instanceof Object[]) {
                throw new IllegalArgumentException();
            }

            if (peers.contains(peer)) {
                peers.remove(peer);

                if (!(peer instanceof ItemGuid)) {
                    if (!(peer instanceof ItemImpl)) {
                        removeRepresentingItems(peer);
                    }

                    if (!anyMoreItemInstanceOfSameKind(peer)) {
                        state(peer).removeFrom(item);
                    }
                }
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
    }

    public boolean anyMoreItemInstanceOfSameKind(Object value) {
        return instancesOfSameKind(value) > 0;
    }

    public long instancesOfSameKind(Object value) {
        Object requested = value instanceof Item ? ((ItemImpl) value).item : value;
        return peers.stream().filter(peer -> {
            return (peer instanceof ItemImpl && ((ItemImpl) peer).item == requested);
        }).count();
    }

    private void removeRepresentingItems(Object value) {
        for (Object peer : new HashSet<>(peers)) {
            if (peer instanceof ItemImpl) {
                ItemImpl itemImpl = (ItemImpl) peer;
                if (QualifiedItem.of(itemImpl.item).equals(QualifiedItem.of(value))) {
                    peers.remove(itemImpl);
                    itemImpl.releaseInstanceGuid();
                }
            }
        }
    }

    private void updatePersistence() {
        persistApplied();
        persistDuration();
        persistPeers();
        persistAttributes();
    }

    private boolean allPeersAreTemporary() {
        for (Object peer : peers) {
            if (peer instanceof ItemGuid) {
                continue;
            } else if (state(peer).isPersisted()) {
                return false;
            }
        }
        return true;
    }

    private boolean isPersisted() {
        return appliedStorage.available();
    }

    private void removePersistence() {
        appliedStorage.clear();
        durationStorage.clear();
        peerStorage.clear();
        attributeStorage.clear();
    }

    private void setApplied() {
        applied = true;
    }

    private void setRemoved() {
        applied = false;
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
        result = prime * result + ((appliedStorage == null) ? 0 : appliedStorage.hashCode());
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
        if (appliedStorage == null) {
            if (other.appliedStorage != null)
                return false;
        } else if (!appliedStorage.equals(other.appliedStorage))
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