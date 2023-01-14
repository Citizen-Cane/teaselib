package teaselib.core;

import static java.util.Collections.unmodifiableSet;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toSet;
import static teaselib.core.StateImpl.Internal.DEFAULT_DOMAIN_NAME;
import static teaselib.core.StateImpl.Internal.PERSISTED_DOMAINS_STATE;
import static teaselib.core.TeaseLib.DefaultDomain;
import static teaselib.core.util.QualifiedStringMapping.map;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Duration;
import teaselib.State;
import teaselib.core.state.AbstractProxy;
import teaselib.core.util.QualifiedString;
import teaselib.core.util.QualifiedStringMapping;
import teaselib.core.util.ReflectionUtils;
import teaselib.util.Item;

public class StateImpl implements State, State.Options, State.Attributes {
    private static final Logger logger = LoggerFactory.getLogger(StateImpl.class);

    static final String FOREVER_KEYWORD = "FOREVER";

    static class Internal {
        static final String PERSISTED_DOMAINS_STATE = ReflectionUtils.qualified("teaselib", "PersistedDomains");
        static final String DEFAULT_DOMAIN_NAME = ReflectionUtils.qualified("teaselib", "DefaultDomain");

        static final Set<QualifiedString> ExcludeFromDurationCalculation = new HashSet<>(
                Arrays.asList(QualifiedString.of(Until.Removed), QualifiedString.of(Until.Expired)));
    }

    static class Domain {
        static final String LAST_USED = ReflectionUtils.qualified("teaselib", "LastUsed");
    }

    public static class Precondition {

        public static Collection<Object> apply(Collection<Object> values) {
            for (Object value : values) {
                if (value instanceof String || value instanceof Enum<?> || value instanceof Item
                        || value instanceof State || value instanceof QualifiedString) {
                    continue;
                } else if (value instanceof Class<?>) {
                    throw new IllegalArgumentException("Class items can only be queried: " + value);
                } else {
                    handleIllegalArgument(value);
                }
            }
            return values;
        }

        public static Collection<Object> remove(Collection<Object> values) {
            return apply(values);
        }

        public static Collection<Object> is(Collection<Object> values) {
            for (Object value : values) {
                if (value instanceof String || value instanceof Enum<?> || value instanceof Item
                        || value instanceof State || value instanceof QualifiedString || isEnumClass(value)) {
                    continue;
                } else {
                    handleIllegalArgument(value);
                }
            }
            return values;
        }

        private static void handleIllegalArgument(Object value) {
            if (value instanceof Class<?> clazz) {
                throw new IllegalArgumentException(value.getClass().getSimpleName() + " is not an Enum: " + value);
            } else {
                throw new IllegalArgumentException(value.getClass().getSimpleName() + ":" + value);
            }
        }

        private static boolean isEnumClass(Object value) {
            return value instanceof Class<?> && ((Class<?>) value).isEnum();
        }
    }

    public final StateMaps cache;
    public final String domain;
    public final QualifiedString name;
    final StateStorage storage;

    boolean applied;
    Duration duration;
    final Set<QualifiedString> peers;
    final Set<QualifiedString> attributes;

    StateImpl(StateMaps stateMaps, String domain, QualifiedString name) {
        StateMaps.checkNotItem(name);

        this.cache = stateMaps;
        this.domain = domain;
        this.name = name;
        this.storage = new StateStorage(this, domain, this.name.toString());

        this.applied = storage.restoreApplied();
        this.duration = storage.restoreDuration();
        this.peers = storage.restorePeers();
        this.attributes = storage.restoreAttributes();

    }

    protected StateImpl(TeaseLib teaseLib, String domain, QualifiedString name) {
        this(teaseLib.stateMaps, domain, name);
    }

    protected StateImpl state(QualifiedString name) {
        return cache.state(domain, name);
    }

    protected StateImpl state(String domain, QualifiedString name) {
        return cache.state(domain, name);
    }

    public static Options infiniteDurationWhenRememberedWithoutDuration(State state, State.Options options) {
        return new Options() {

            @Override
            public void remember(Until forget) {
                if (forget == Until.Removed) {
                    over(Duration.INFINITE, TimeUnit.SECONDS);
                } else if (state.duration().limit(TimeUnit.SECONDS) == 0) {
                    throw new IllegalArgumentException("Trying to remember " + forget + " without explicit duration.");
                }
                options.remember(forget);
            }

            @Override
            public Persistence over(long duration, TimeUnit unit) {
                return options.over(duration, unit);
            }

            @Override
            public Persistence over(Duration duration) {
                return options.over(duration);
            }

        };
    }

    @Override
    public Options apply() {
        applyImpl(Collections.emptySet());
        return infiniteDurationWhenRememberedWithoutDuration(this, this);
    }

    @Override
    public State.Options applyTo(Object... attributes) {
        applyImpl(QualifiedStringMapping.map(Precondition::apply, QualifiedStringMapping::reduceItemGuidsToStates,
                attributes));
        return infiniteDurationWhenRememberedWithoutDuration(this, this);
    }

    public State.Options applyImpl(Set<QualifiedString> attributes) {
        if (!applied()) {
            setTemporary();
        }

        for (QualifiedString attribute : attributes) {
            if (!peersContain(attribute)) {
                if (attribute.name().equals(QualifiedString.ANY)) {
                    throw new IllegalStateException("Class selectors cannot be applied: " + attribute);
                }
                peers.add(attribute);
                if (!attribute.isItem()) {
                    StateImpl state = state(attribute);
                    state.applyImpl(Collections.singleton(name));
                }
            }
        }

        setApplied();
        updateLastUsed(duration);
        return this;
    }

    public List<StateImpl> peerStates() {
        return states(new HashSet<>(peers));
    }

    private List<StateImpl> states(Collection<QualifiedString> elements) {
        return elements.stream().filter(peer -> QualifiedString.of(peer).guid().isEmpty()).map(this::state).toList();
    }

    private void setTemporary() {
        over(TEMPORARY, SECONDS);
    }

    public Set<QualifiedString> peers() {
        return unmodifiableSet(peers);
    }

    @Override
    public void applyAttributes(Object... attributes) {
        applyAttributesImpl(map(Precondition::apply, attributes));
    }

    void applyAttributesImpl(Set<QualifiedString> attributes) {
        this.attributes.addAll(attributes);
    }

    public Set<QualifiedString> getAttributes() {
        return unmodifiableSet(attributes);
    }

    @Override
    public boolean is(Object... attributes) {
        return isImpl(map(Precondition::is, attributes));
    }

    public boolean isImpl(Set<QualifiedString> flattenedAttributes) {
        Set<QualifiedString> attributesAndPeers = attributesAndPeers();
        return isImpl(flattenedAttributes, attributesAndPeers);
    }

    public boolean isImpl(Set<QualifiedString> flattenedAttributes, Set<QualifiedString> attributesAndPeers) {
        for (QualifiedString element : flattenedAttributes) {
            if (!isImpl(attributesAndPeers, element)) {
                return false;
            }
        }
        return true;
    }

    public boolean isImpl(Set<QualifiedString> elements, QualifiedString element) {
        if (element.name().equals(QualifiedString.ANY)) {
            if (!haveClass(elements, element))
                return false;
        } else if (element.isItem()) {
            if (!haveItem(elements, element)) {
                return false;
            }
        } else if (!haveAttribute(elements, element) && !element.equals(this.name)) {
            return false;
        }
        return true;
    }

    public static boolean haveClass(Set<QualifiedString> attributesAndPeers, QualifiedString element) {
        String clazz = element.namespace();
        return attributesAndPeers.stream().map(QualifiedString::namespace)
                .anyMatch(namespace -> namespace.equalsIgnoreCase(clazz));
    }

    public static boolean haveItem(Set<QualifiedString> attributesAndPeers, QualifiedString element) {
        List<QualifiedString> guids = attributesAndPeers.stream().filter(QualifiedString::isItem).toList();
        return guids.contains(element);
    }

    public static boolean haveAttribute(Set<QualifiedString> attributesAndPeers, QualifiedString element) {
        return attributesAndPeers.stream().anyMatch(element::is);
    }

    public Set<QualifiedString> attributesAndPeers() {
        Stream<QualifiedString> myAttributeAndPeers = Stream.concat(peers.stream(), attributes.stream());
        Stream<QualifiedString> attributesOfDirectPeers = peerStates().stream().map(state -> state.attributes)
                .flatMap(Set::stream);
        return Stream.concat(myAttributeAndPeers, attributesOfDirectPeers).collect(toSet());
    }

    @Override
    public State.Persistence over(long limit, TimeUnit unit) {
        return over(new DurationImpl(this.cache.teaseLib, limit, unit));
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
            var relevant = peers.stream().filter(Predicate.not(Internal.ExcludeFromDurationCalculation::contains)).toList();
            var durations = states(relevant).stream().map(state -> state.duration).toList();
            Optional<Duration> maximum = Stream.concat(Stream.of(this.duration), durations.stream())
                    .max((a, b) -> Long.compare(a.remaining(TimeUnit.SECONDS), b.remaining(TimeUnit.SECONDS)));
            if (maximum.isPresent()) {
                return maximum.get();
            } else {
                return duration;
            }
        } else {
            return state(Domain.LAST_USED, name).duration();
        }
    }

    @Override
    public void remember(Until forget) {
        if (!name.toString().equalsIgnoreCase(PERSISTED_DOMAINS_STATE)) {
            applyTo(forget);
            var state = cache.teaseLib.state(DefaultDomain, PERSISTED_DOMAINS_STATE);
            String peer = domain.equals(DefaultDomain) ? DEFAULT_DOMAIN_NAME : domain;
            state.applyTo(peer).over(Duration.INFINITE, MILLISECONDS).remember(Until.Removed);
        }

        storage.updatePersistence();
        for (QualifiedString peer : peers) {
            if (!QualifiedString.isItemGuid(peer)) {
                StateImpl peerState = state(peer);
                peerState.storage.updatePersistence();
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
            for (QualifiedString peer : peers) {
                if (!peer.isItem()) {
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
        remove(duration);
    }

    void remove(Duration duration) {
        removeState(duration);
        updateLastUsed(duration);
    }

    private void removeState(Duration duration) {
        if (!peers.isEmpty()) {
            var copyOfPeers = new QualifiedString[peers.size()];
            for (QualifiedString peer : peers.toArray(copyOfPeers)) {
                if (!peer.isItem()) {
                    state(peer).removeFromImpl(Collections.singleton(name), duration);
                }
            }
            peers.clear();
        }

        attributes.clear();
        setRemoved();
        if (storage.persisted()) {
            storage.deletePersistence();
        }
    }

    void updateLastUsed(Duration duration) {
        if (!Domain.LAST_USED.equals(domain)) {
            updateLastUsed(new FrozenDuration(cache.teaseLib, duration));
        }
    }

    private void updateLastUsed(FrozenDuration frozenDuration) {
        // TODO apply with domain name
        state(Domain.LAST_USED, name).applyImpl(Collections.emptySet()).over(frozenDuration).remember(Until.Removed);
    }

    @Override
    public void removeFrom(Object... peers) {
        if (peers.length == 0) {
            throw new IllegalArgumentException("removeFrom requires at least one peer");
        }

        Set<QualifiedString> flattenedPeers = QualifiedStringMapping.map(Precondition::remove,
                QualifiedStringMapping::reduceItemGuidsToStates, peers);
        removeFromImpl(flattenedPeers, duration);
    }

    public void removeFromImpl(Set<QualifiedString> flattenedPeers, Duration duration) {
        for (QualifiedString peer : flattenedPeers) {
            if (peersContain(peer)) {
                // TODO remove only those peers not available in any other applied item
                removePeer(peer);

                if (!QualifiedString.isItemGuid(peer)) {
                    removeRepresentingGuids(peer);
                }

                if (guidsOfSameKind(peer.kind()) == 0) {
                    // reverse callback to resolve peering
                    state(peer.kind()).removeFromImpl(Collections.singleton(name), duration);
                }
            }

            if (allPeersAreTemporary() && storage.persisted()) {
                storage.deletePersistence();
            }
        }

        if (peers.isEmpty()) {
            remove(duration);
        } else if (containOnlyBookkeepingStates()) {
            remove(duration);
        } else if (storage.persisted()) {
            storage.updatePersistence();
        }
    }

    private boolean containOnlyBookkeepingStates() {
        return peers.stream().noneMatch(peer -> {
            var untilClass = QualifiedString.of(Until.class);
            String namespace = untilClass.namespace();
            return !peer.namespace().equals(namespace);
        });
    }

    private boolean peersContain(QualifiedString peer) {
        return peers.stream().anyMatch(peer::is);
    }

    private boolean removePeer(QualifiedString peer) {
        return peers.removeIf(peer::equals);
    }

    public long guidsOfSameKind(QualifiedString item) {
        return peers.stream().filter(QualifiedString::isItem).map(QualifiedString::kind).filter(item.kind()::equals)
                .count();
    }

    private void removeRepresentingGuids(QualifiedString value) {
        for (QualifiedString peer : new HashSet<>(peers)) {
            if (peer.isItem()) {
                if (peer.kind().is(value)) {
                    peers.remove(peer);
                    ItemImpl peerItem;
                    try {
                        peerItem = getItem(peer);
                        peerItem.releaseInstanceGuid(duration);
                    } catch (NoSuchElementException e) {
                        logger.warn("Item {} does not exist anymore: {}", peer, e.getMessage());
                    }
                }
            }
        }
    }

    private ItemImpl getItem(QualifiedString guid) {
        return (ItemImpl) cache.teaseLib.getItem(domain, guid);
    }

    private boolean allPeersAreTemporary() {
        for (QualifiedString peer : peers) {
            if (peer.isItem()) {
                continue;
            } else if (state(peer).storage.persisted()) {
                return false;
            }
        }
        return true;
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
        var toString = "name=" + (domain.isEmpty() ? "" : domain + ".") + name.name();
        return toString + " " + duration + " peers=" + peers;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.cache.hashCode();
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result + ((domain == null) ? 0 : domain.hashCode());
        result = prime * result + ((duration == null) ? 0 : duration.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (peers == null) {
            if (other.peers != null)
                return false;
        } else if (!peers.equals(other.peers))
            return false;
        return true;
    }

}