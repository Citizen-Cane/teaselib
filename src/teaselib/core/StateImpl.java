package teaselib.core;

import static java.util.concurrent.TimeUnit.*;
import static java.util.stream.Collectors.*;
import static teaselib.core.StateImpl.Internal.*;
import static teaselib.core.TeaseLib.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Duration;
import teaselib.State;
import teaselib.core.TeaseLib.PersistentBoolean;
import teaselib.core.TeaseLib.PersistentString;
import teaselib.core.state.AbstractProxy;
import teaselib.core.util.Persist;
import teaselib.core.util.PersistedObject;
import teaselib.core.util.QualifiedString;
import teaselib.core.util.ReflectionUtils;
import teaselib.core.util.Storage;
import teaselib.util.Item;
import teaselib.util.ItemGuid;
import teaselib.util.ItemImpl;

public class StateImpl implements State, State.Options, StateMaps.Attributes {
    private static final Logger logger = LoggerFactory.getLogger(StateImpl.class);

    private static final String FOREVER_KEYWORD = "FOREVER";

    static class Internal {
        static final String PERSISTED_DOMAINS_STATE = ReflectionUtils.qualified("teaselib", "PersistedDomains");
        static final String DEFAULT_DOMAIN_NAME = ReflectionUtils.qualified("teaselib", "DefaultDomain");
    }

    static class Domain {
        static final String LAST_USED = ReflectionUtils.qualified("teaselib", "LastUsed");
    }

    private final StateMaps stateMaps;
    public final String domain;
    public final QualifiedString item;

    private final Set<Object> peers = new HashSet<>();
    private final Set<QualifiedString> attributes = new HashSet<>();
    private boolean applied = false;

    static class StateStorage {
        final PersistentBoolean appliedStorage;
        final PersistentString durationStorage;
        final PersistentString peerStorage;
        final PersistentString attributeStorage;

        StateStorage(TeaseLib teaseLib, String domain, String item) {
            this.appliedStorage = persistentApplied(teaseLib, domain, item);
            this.durationStorage = persistentDuration(teaseLib, domain, item);
            this.peerStorage = persistentPeers(teaseLib, domain, item);
            this.attributeStorage = persistentAttributes(teaseLib, domain, item);
        }

        TeaseLib.PersistentBoolean persistentApplied(TeaseLib teaseLib, String domain, String item) {
            return persistentBoolean(teaseLib, domain, item, "applied");
        }

        TeaseLib.PersistentString persistentDuration(TeaseLib teaseLib, String domain, String item) {
            return persistentString(teaseLib, domain, item, "duration");
        }

        PersistentString persistentPeers(TeaseLib teaseLib, String domain, String item) {
            return persistentString(teaseLib, domain, item, "peers");
        }

        PersistentString persistentAttributes(TeaseLib teaseLib, String domain, String item) {
            return persistentString(teaseLib, domain, item, "attributes");
        }

        private static PersistentBoolean persistentBoolean(TeaseLib teaseLib, String domain, String item, String name) {
            return teaseLib.new PersistentBoolean(domain, item, "state." + name);
        }

        private static PersistentString persistentString(TeaseLib teaseLib, String domain, String item, String name) {
            return teaseLib.new PersistentString(domain, item, "state." + name);
        }

        void deletePersistence() {
            appliedStorage.clear();
            durationStorage.clear();
            peerStorage.clear();
            attributeStorage.clear();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((appliedStorage == null) ? 0 : appliedStorage.hashCode());
            result = prime * result + ((attributeStorage == null) ? 0 : attributeStorage.hashCode());
            result = prime * result + ((durationStorage == null) ? 0 : durationStorage.hashCode());
            result = prime * result + ((peerStorage == null) ? 0 : peerStorage.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            StateStorage other = (StateStorage) obj;
            if (appliedStorage == null) {
                if (other.appliedStorage != null)
                    return false;
            } else if (!appliedStorage.equals(other.appliedStorage))
                return false;
            if (attributeStorage == null) {
                if (other.attributeStorage != null)
                    return false;
            } else if (!attributeStorage.equals(other.attributeStorage))
                return false;
            if (durationStorage == null) {
                if (other.durationStorage != null)
                    return false;
            } else if (!durationStorage.equals(other.durationStorage))
                return false;
            if (peerStorage == null) {
                if (other.peerStorage != null)
                    return false;
            } else if (!peerStorage.equals(other.peerStorage))
                return false;
            return true;
        }
    }

    private final StateStorage storage;

    private Duration duration;

    protected StateImpl state(Object item) {
        if (item instanceof AbstractProxy<?>) {
            return state(((AbstractProxy<?>) item).state);
        } else if (item instanceof ItemImpl) {
            return state(((ItemImpl) item).value());
        } else if (item instanceof StateImpl) {
            return (StateImpl) item;
        } else {
            return (StateImpl) this.stateMaps.state(domain, item);
        }
    }

    protected StateImpl state(String domain, Object item) {
        if (item instanceof AbstractProxy<?>) {
            return state(domain, ((AbstractProxy<?>) item).state);
        } else if (item instanceof ItemImpl) {
            return state(domain, ((ItemImpl) item).value());
        } else if (item instanceof StateImpl) {
            StateImpl state = (StateImpl) item;
            if (state.domain.equals(domain)) {
                return state;
            } else {
                throw new IllegalArgumentException("StateImpl domain mismatch");
            }
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
        this.item = QualifiedString.of(item);
        this.storage = new StateStorage(this.stateMaps.teaseLib, domain, this.item.toString());

        restoreApplied();
        restoreDuration();
        restoreAttributes();
        restorePeers();
    }

    protected StateImpl(TeaseLib teaseLib, String domain, Object item) {
        this(teaseLib.stateMaps, domain, item);
    }

    private void restoreApplied() {
        applied = storage.appliedStorage.value();
    }

    private void restoreDuration() {
        if (isPersisted()) {
            // TODO Refactor persistence to duration class
            String[] argv = storage.durationStorage.value().split(" ");
            long start = Long.parseLong(argv[0]);
            long limit = argv.length > 1 ? string2timespan(argv[1]) : 0;
            if (argv.length == 1) {
                this.duration = new DurationImpl(this.stateMaps.teaseLib, start, 0, TimeUnit.SECONDS);
            } else if (argv.length == 2) {
                this.duration = new DurationImpl(this.stateMaps.teaseLib, start, limit, TimeUnit.SECONDS);
            } else if (argv.length == 3) {
                long elapsed = string2timespan(argv[2]);
                this.duration = new FrozenDuration(stateMaps.teaseLib, start, limit, elapsed, TimeUnit.SECONDS);
            } else {
                throw new IllegalStateException(storage.durationStorage.value());
            }
        } else {
            this.duration = new FrozenDuration(this.stateMaps.teaseLib, 0, 0, TimeUnit.SECONDS);
        }
    }

    private static long string2timespan(String limitString) {
        long limit;
        if (limitString.equals(FOREVER_KEYWORD)) {
            limit = Duration.INFINITE;
        } else {
            limit = Long.parseLong(limitString);
        }
        return limit;
    }

    private void restorePeers() {
        if (storage.peerStorage.available()) {
            String persisted = storage.peerStorage.value();
            List<String> presistedPeers = new PersistedObject(ArrayList.class, persisted).toValues();
            for (String persistedPeer : presistedPeers) {
                restorePersistedPeer(persistedPeer);
            }

            if (peers.isEmpty()) {
                remove();
            }
        }
    }

    private void restorePersistedPeer(String persisted) {
        Object peer;
        try {
            peer = getPersistedPeer(persisted);
            addPeerThatHasBeenPersistedWithMe(peer);
        } catch (NoSuchElementException e) {
            logger.warn("Item {} does not exist anymore: {}", persisted, e.getMessage());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Peer " + persisted + " not restored: " + e.getMessage(), e);
        }
    }

    private Object getPersistedPeer(String peer) throws ReflectiveOperationException {
        if (PersistedObject.className(peer).equals(ItemImpl.class.getName())) {
            return ItemImpl.restoreFromUserItems(stateMaps.teaseLib, domain, Storage.from(peer));
        } else {
            return Persist.from(peer);
        }
    }

    private void addPeerThatHasBeenPersistedWithMe(Object peer) {
        var qualifiedPeer = QualifiedString.of(peer);
        if (storage.persistentDuration(stateMaps.teaseLib, domain, qualifiedPeer.toString()).available()) {
            if (peer instanceof QualifiedString) {
                peers.add(peer);
            } else {
                throw new IllegalStateException(peer.toString());
            }
        } else if (peer instanceof Item) {
            peers.add(peer);
        } else if (peer instanceof ItemGuid) {
            peers.add(peer);
        } else if (qualifiedPeer.guid().isPresent()) {
            peers.add(ItemGuid.fromGuid(peer.toString()));
        } else if (isCached(qualifiedPeer) && state(peer).applied()) {
            if (peer instanceof QualifiedString) {
                peers.add(peer);
            } else {
                throw new IllegalStateException(peer.toString());
            }
        }
    }

    private boolean isCached(QualifiedString qualifiedPeer) {
        return this.stateMaps.stateMap(domain, qualifiedPeer).contains(qualifiedPeer.name().toLowerCase());
    }

    private void restoreAttributes() {
        if (storage.attributeStorage.available()) {
            try {
                attributes.addAll(Persist.from(ArrayList.class, storage.attributeStorage.value()));
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException("Cannot restore attributes for state " + this.item + ": ", e);
            }
        }
    }

    private void persistApplied() {
        storage.appliedStorage.set(applied);
    }

    private void persistDuration() {
        var start = Long.toString(duration.start(TimeUnit.SECONDS));
        long limitValue = duration.limit(TimeUnit.SECONDS);
        var limit = timespan2String(limitValue);
        if (duration instanceof FrozenDuration) {
            var elapsed = timespan2String(duration.elapsed(TimeUnit.SECONDS));
            storage.durationStorage.set(start + " " + limit + " " + elapsed);
        } else {
            if (limitValue != 0) {
                storage.durationStorage.set(start + " " + limit);
            } else {
                storage.durationStorage.set(start);
            }
        }
    }

    private static String timespan2String(long timespan) {
        String timespanString;
        if (timespan < 0) {
            timespanString = Long.toString(0);
        } else if (timespan == Duration.INFINITE) {
            timespanString = FOREVER_KEYWORD;
        } else {
            timespanString = Long.toString(timespan);
        }
        return timespanString;
    }

    private void persistPeers() {
        if (peers.isEmpty()) {
            storage.peerStorage.clear();
        } else {
            storage.peerStorage.set(Persist.persistValues(peers));
        }
    }

    private void persistAttributes() {
        if (attributes.isEmpty()) {
            storage.attributeStorage.clear();
        } else {
            storage.attributeStorage.set(Persist.persistValues(attributes));
        }
    }

    @Override
    public Options apply() {
        return applyTo();
    }

    @Override
    public State.Options applyTo(Object... attributes) {
        applyInternal(mapToQualifiedString(StateMaps.flatten(AbstractProxy.removeProxies(attributes))));
        return this;
    }

    public static List<Object> mapToQualifiedString(List<Object> values) {
        List<Object> mapped = new ArrayList<>(values.size());
        for (Object value : values) {
            if (value instanceof Class || value instanceof ItemGuid || value instanceof Item
                    || value instanceof QualifiedString) {
                mapped.add(value);
            } else {
                var s = QualifiedString.of(value);
                mapped.add(s);
            }
        }
        return mapped;
    }

    public static Set<QualifiedString> mapToQualifiedStringTyped(Collection<Object> values) {
        Set<QualifiedString> mapped = new HashSet<>(values.size());
        for (Object value : values) {
            if (value instanceof Class || value instanceof ItemGuid || value instanceof Item) {
                throw new IllegalArgumentException(value.toString());
            } else if (value instanceof QualifiedString) {
                mapped.add((QualifiedString) value);
            } else {
                var s = QualifiedString.of(value);
                mapped.add(s);
            }
        }
        return mapped;
    }

    private State applyInternal(List<Object> attributes) {
        if (!applied()) {
            setTemporary();
        }

        for (Object attribute : attributes) {
            if (!peersContain(attribute)) {
                if (attribute instanceof List || attribute.getClass().isArray()) {
                    throw new IllegalStateException(attribute.toString());
                } else if (attribute instanceof Class<?>) {
                    throw new IllegalStateException("Class items cannot be applied: " + attribute);
                } else if (attribute instanceof QualifiedString
                        && ((QualifiedString) attribute).name().equals(QualifiedString.ANY)) {
                    throw new IllegalStateException("Class selectors cannot be applied: " + attribute);
                }
                peers.add(attribute);
                if (!(attribute instanceof ItemGuid)) {
                    StateImpl state = state(attribute);
                    state.applyInternal(Collections.singletonList(item));
                }
            }
        }

        setApplied();
        updateLastUsed();
        return this;
    }

    public List<StateImpl> peerStates() {
        return states(new HashSet<>(peers));
    }

    private List<StateImpl> states(Collection<? extends Object> elements) {
        return elements.stream().filter(peer -> QualifiedString.of(peer).guid().isEmpty()).map(this::state)
                .collect(toList());
    }

    private void setTemporary() {
        over(TEMPORARY, SECONDS);
    }

    public Set<Object> peers() {
        return Collections.unmodifiableSet(peers);
    }

    @Override
    public void applyAttributes(Object... attributes) {
        applyAttributesImpl(mapToQualifiedStringTyped(StateMaps.flatten(attributes)));
    }

    void applyAttributesImpl(Set<QualifiedString> attributes) {
        this.attributes.addAll(attributes);
    }

    public Set<Object> getAttributes() {
        return Collections.unmodifiableSet(attributes);
    }

    @Override
    // TODO classes and instances in a single query
    public boolean is(Object... attributes) {
        List<Object> flattenedAttributes = mapToQualifiedString(
                StateMaps.flatten(AbstractProxy.removeProxies(attributes)));
        return isImpl(flattenedAttributes);
    }

    public boolean isImpl(List<Object> flattenedAttributes) {
        Set<Object> attributesAndPeers = attributesAndPeers();
        return isImpl(flattenedAttributes, attributesAndPeers);
    }

    private boolean isImpl(List<Object> flattenedAttributes, Set<Object> attributesAndPeers) {
        if (appliedToClassValues(attributesAndPeers, flattenedAttributes)) {
            return true;
        } else if (appliedToClassState(attributesAndPeers, flattenedAttributes)) {
            return true;
        } else if (!allItemInstancesFoundInPeers(flattenedAttributes)) {
            return false;
        } else if (StateMaps.hasAllAttributes(attributesAndPeers, flattenedAttributes)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean appliedToClassState(Set<Object> availableAttributes, List<Object> desiredAttributes) {
        return appliedToClass(states(availableAttributes), desiredAttributes);

    }

    public boolean appliedToClassValues(Set<? extends Object> availableAttributes, List<Object> desiredAttributes) {
        return appliedToClass(availableAttributes, desiredAttributes);
    }

    private static boolean appliedToClass(Collection<? extends Object> available, List<Object> desired) {
        List<Class<?>> classes = desired.stream().filter(Class.class::isInstance).map(Class.class::cast)
                .collect(toList());
        return classes.stream()
                .filter(desiredAttribute -> available.stream().filter(QualifiedString.class::isInstance)
                        .map(QualifiedString.class::cast).map(QualifiedString::namespace)
                        .anyMatch(namespace -> namespace.equalsIgnoreCase(ReflectionUtils.qualified(desiredAttribute))))
                .count() == desired.size();

    }

    private boolean allItemInstancesFoundInPeers(List<Object> attributes) {
        List<ItemImpl> instances = attributes.stream().filter(ItemImpl.class::isInstance).map(ItemImpl.class::cast)
                .collect(toList());
        List<ItemGuid> guids = instances.stream().map(instance -> instance.guid).collect(Collectors.toList());
        return peers.containsAll(instances) || peers.containsAll(guids);
    }

    private Set<Object> attributesAndPeers() {
        Stream<Object> myAttributeAndPeers = Stream.concat(peers.stream(), attributes.stream());
        Stream<Object> attributesOfDirectPeers = peerStates().stream().map(state -> state.attributes)
                .flatMap(Set::stream);
        return Stream.concat(myAttributeAndPeers, attributesOfDirectPeers).collect(Collectors.toSet());
    }

    @Override
    public State.Persistence over(long limit, TimeUnit unit) {
        return over(new DurationImpl(this.stateMaps.teaseLib, limit, unit));
    }

    @Override
    public Persistence over(Duration duration) {
        this.duration = duration;
        updateLastUsed(this.duration);
        return this;
    }

    @Override
    public Duration duration() {
        if (applied || Domain.LAST_USED.equals(domain)) {
            Stream<Duration> durations = peerStates().stream().map(state -> state.duration);
            Optional<Duration> maximum = Stream.concat(Stream.of(this.duration), durations)
                    .max((a, b) -> Long.compare(a.remaining(TimeUnit.SECONDS), b.remaining(TimeUnit.SECONDS)));
            if (maximum.isPresent()) {
                return maximum.get();
            } else {
                return duration;
            }
        } else {
            return state(Domain.LAST_USED, item).duration();
        }
    }

    @Override
    public void remember(Until forget) {
        if (!item.toString().equalsIgnoreCase(PERSISTED_DOMAINS_STATE)) {
            applyTo(forget);
            var state = stateMaps.teaseLib.state(DefaultDomain, PERSISTED_DOMAINS_STATE);
            Object peer = domain.equals(DefaultDomain) ? DEFAULT_DOMAIN_NAME : domain;
            state.applyTo(peer).over(Duration.INFINITE, MILLISECONDS).remember(Until.Removed);
        }

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
            var copyOfPeers = new Object[peers.size()];
            for (Object peer : peers.toArray(copyOfPeers)) {
                if (peer instanceof ItemGuid) {
                    peers.remove(peer);
                } else {
                    state(peer).removeFrom(Collections.singletonList(item));
                }
            }
            peers.clear();
        }

        attributes.clear();
        setRemoved();
        if (isPersisted()) {
            storage.deletePersistence();
        }

        updateLastUsed(this.duration);
    }

    public void updateLastUsed(Duration duration) {
        if (!Domain.LAST_USED.equals(domain)) {
            updateLastUsed(new FrozenDuration(stateMaps.teaseLib, duration));
        }
    }

    public void updateLastUsed() {
        if (!Domain.LAST_USED.equals(domain)) {
            long now = stateMaps.teaseLib.getTime(TeaseLib.DURATION_TIME_UNIT);
            // TODO update with actual duration value
            long limit = State.TEMPORARY;
            long elapsed = 0;
            updateLastUsed(new FrozenDuration(stateMaps.teaseLib, now, limit, elapsed, TeaseLib.DURATION_TIME_UNIT));
        }
    }

    private void updateLastUsed(FrozenDuration frozenDuration) {
        // TODO apply with domain name
        state(Domain.LAST_USED, item).apply().over(frozenDuration).remember(Until.Removed);
    }

    @Override
    public void removeFrom(Object... peers2) {
        if (peers2.length == 0) {
            throw new IllegalArgumentException("removeFrom requires at least one peer");
        }

        List<Object> flattenedPeers = mapToQualifiedString(StateMaps.flatten(AbstractProxy.removeProxies(peers2)));
        removeFrom(flattenedPeers);
    }

    private void removeFrom(List<Object> flattenedPeers) {
        for (Object peer : flattenedPeers) {
            if (peer instanceof Collection<?> || peer instanceof Object[]) {
                throw new IllegalArgumentException();
            }

            if (peersContain(peer)) {
                removePeer(peer);

                if (!(peer instanceof ItemGuid)) {
                    if (!(peer instanceof ItemImpl)) {
                        removeRepresentingItems((QualifiedString) peer);
                    }

                    if (!anyMoreItemInstanceOfSameKind(peer)) {
                        state(peer).removeFrom(Collections.singletonList(item));
                    }
                }
            }

            if (allPeersAreTemporary() && isPersisted()) {
                storage.deletePersistence();
            }
        }

        if (peers.isEmpty()) {
            remove();
        } else if (containOnlyBookkeepingStates()) {
            remove();
        } else if (isPersisted()) {
            updatePersistence();
        }
    }

    private boolean containOnlyBookkeepingStates() {
        return peers.stream().noneMatch(peer -> {
            var untilClass = QualifiedString.of(Until.class);
            String namespace = untilClass.namespace();
            return !QualifiedString.of(peer).namespace().equals(namespace);
        });
    }

    private boolean peersContain(Object peer) {
        if (peer instanceof Item) {
            return peers.stream().filter(Item.class::isInstance).anyMatch(peer::equals);
        } else if (peer instanceof ItemGuid) {
            var qualifiedPeer = QualifiedString.of(peer);
            return peers.stream().anyMatch(qualifiedPeer::is);
        } else if (peer instanceof QualifiedString) {
            var qualifiedPeer = (QualifiedString) peer;
            return peers.stream().anyMatch(qualifiedPeer::is);
        } else {
            throw new IllegalArgumentException(peer.toString());
        }
    }

    private boolean removePeer(Object peer) {
        return peers.removeIf(p -> {
            return peer.equals(p);
        });
    }

    public boolean anyMoreItemInstanceOfSameKind(Object value) {
        return instancesOfSameKind(value) > 0;
    }

    public long instancesOfSameKind(Object value) {
        Object requested = value instanceof Item ? ((ItemImpl) value).value() : value;
        return peers.stream().filter(peer -> peer instanceof ItemImpl && ((ItemImpl) peer).value().equals(requested))
                .count();
    }

    private void removeRepresentingItems(QualifiedString value) {
        for (Object peer : new HashSet<>(peers)) {
            if (peer instanceof ItemImpl) {
                var itemImpl = (ItemImpl) peer;
                if (itemImpl.value().is(value)) {
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
        return storage.appliedStorage.available();
    }

    private void setApplied() {
        applied = true;
    }

    private void setRemoved() {
        applied = false;
    }

    @Override
    public boolean removed() {
        return !applied();
    }

    @Override
    public long removed(TimeUnit unit) {
        if (applied()) {
            return 0L;
        } else {
            return duration().since(unit);
        }
    }

    @Override
    public String toString() {
        String name = "name=" + (domain.isEmpty() ? "" : domain + ".") + item.name();
        return name + " " + duration + " peers=" + StateMaps.toStringWithoutRecursion(peers);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.stateMaps.hashCode();
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result + ((domain == null) ? 0 : domain.hashCode());
        result = prime * result + ((duration == null) ? 0 : duration.hashCode());
        result = prime * result + ((item == null) ? 0 : item.hashCode());
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

    public boolean generatedEqualsMethod(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StateImpl other = (StateImpl) obj;
        if (applied != other.applied)
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
        if (item == null) {
            if (other.item != null)
                return false;
        } else if (!item.equals(other.item))
            return false;
        if (peers == null) {
            if (other.peers != null)
                return false;
        } else if (!peers.equals(other.peers))
            return false;
        return true;
    }

}