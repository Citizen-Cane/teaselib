package teaselib.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import teaselib.Duration;
import teaselib.State;
import teaselib.core.StateImpl;
import teaselib.core.StateImpl.Preconditions;
import teaselib.core.StateMaps;
import teaselib.core.TeaseLib;
import teaselib.core.state.AbstractProxy;
import teaselib.core.util.Persist;
import teaselib.core.util.Persist.Persistable;
import teaselib.core.util.QualifiedString;
import teaselib.core.util.ReflectionUtils;
import teaselib.core.util.Storage;

/**
 * @author Citizen-Cane
 *
 */
public class ItemImpl implements Item, State.Options, StateMaps.Attributes, Persistable {

    public static final String Available = "Available";

    final TeaseLib teaseLib;

    public final String domain;
    public final QualifiedString guid;
    public final String displayName;
    private final TeaseLib.PersistentBoolean available;
    public final Set<QualifiedString> defaultPeers;
    public final Set<QualifiedString> attributes;

    public static String createDisplayName(QualifiedString item) {
        return item.guid().orElseThrow().replace("_", " ");
    }

    public ItemImpl(TeaseLib teaseLib, String domain, QualifiedString guid, String displayName) {
        this(teaseLib, domain, guid, displayName, new Object[] {}, new Object[] {});
    }

    public ItemImpl(TeaseLib teaseLib, String domain, QualifiedString guid, String displayName, Object[] defaultPeers,
            Object[] attributes) {
        this.teaseLib = teaseLib;
        this.domain = domain;
        this.guid = guid;
        this.displayName = displayName;
        this.available = teaseLib.new PersistentBoolean(domain, kind().toString(),
                guid.guid().orElseThrow() + "." + Available);
        this.defaultPeers = Collections
                .unmodifiableSet(StateImpl.mapToQualifiedStringTyped(Arrays.asList(defaultPeers)));
        this.attributes = Collections.unmodifiableSet(
                StateImpl.mapToQualifiedStringTyped(attributes(guid.kind(), Arrays.asList(attributes))));
    }

    public QualifiedString kind() {
        return guid.kind();
    }

    private StateImpl state() {
        return state(kind());
    }

    public static ItemImpl restoreFromUserItems(TeaseLib teaseLib, String domain, Storage storage)
            throws ReflectiveOperationException {
        var item = new QualifiedString(storage.next());
        return (ItemImpl) teaseLib.getItem(domain, item.kind(), item.guid().orElseThrow());
    }

    @Override
    public List<String> persisted() {
        return Arrays.asList(Persist.persist(guid.toString()));
    }

    private static Set<Object> attributes(QualifiedString item, List<Object> attributes) {
        Set<Object> all = new HashSet<>();
        all.add(item);
        all.addAll(attributes);
        return all;
    }

    @Override
    public boolean isAvailable() {
        return available.value();
    }

    @Override
    public void setAvailable(boolean isAvailable) {
        available.set(isAvailable);
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return guid.guid().orElseThrow() + " " + attributes + " " + teaseLib.state(domain, kind()).toString();
    }

    boolean has(List<? extends Object> desired) {
        Stream<QualifiedString> available = Stream.concat(attributes.stream(), defaultPeers.stream());
        return has(available, desired);
    }

    private static boolean has(Stream<? extends QualifiedString> available, List<? extends Object> desired) {
        return available.filter(element -> contains(desired, QualifiedString.of(element))).count() == desired.size();
    }

    static boolean contains(List<? extends Object> elements, QualifiedString value) {
        return elements.stream().anyMatch(value::is);
    }

    @Override
    public boolean is(Object... attributes3) {
        List<Object> flattenedAttributes = StateImpl
                .mapToQualifiedString(StateMaps.flatten(AbstractProxy.removeProxies(attributes3)));
        return isImpl(flattenedAttributes);
    }

    private boolean isImpl(List<Object> flattenedAttributes) {
        if (flattenedAttributes.isEmpty()) {
            return false;
        } else {
            if (has(this.attributes.stream(), flattenedAttributes)) {
                return true;
            } else if (attributeIsMe(flattenedAttributes)) {
                return true;
            } else {
                StateImpl state = state();
                if (StateMaps.hasAllAttributes(state.getAttributes(), flattenedAttributes)) {
                    return applied();
                } else if (state.appliedToClassValues(attributes, flattenedAttributes)) {
                    return true;
                } else if (state.appliedToClassValues(state.peers(), flattenedAttributes)) {
                    return true;
                } else if (!stateAppliesToMe(flattenedAttributes)) {
                    return false;
                } else if (stateContainsAll(flattenedAttributes)) {
                    return true;
                } else {
                    return false;
                }
            }
        }

    }

    private boolean attributeIsMe(List<Object> flattenedAttributes) {
        return flattenedAttributes.stream().filter(ItemImpl.class::isInstance).map(ItemImpl.class::cast)
                .map(item -> item.guid).anyMatch(this.guid::equals);
    }

    private boolean stateAppliesToMe(List<Object> attributes2) {
        return StateMaps.flatten(attributes2).stream().map(this::state).allMatch(this::stateIsThis);
    }

    private boolean stateContainsAll(List<Object> attributes) {
        return state().isImpl(attributes);
    }

    @Override
    public boolean canApply() {
        if (defaultPeers.isEmpty()) {
            return !state().is(this.guid);
        } else {
            return defaultStates().allMatch(state -> !state.applied());
        }
    }

    @Override
    public boolean applied() {
        StateImpl state = state();
        if (state.applied()) {
            if (defaultPeers.isEmpty()) {
                return containsMyGuid(state);
            } else {
                return defaultStates().anyMatch(this::containsMyGuid);
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean expired() {
        return state().expired();
    }

    @Override
    public Duration duration() {
        return state().duration();
    }

    @Override
    public State.Options apply() {
        StateImpl state = state();

        if (defaultPeers.isEmpty()) {
            state.apply();
        } else {
            applyInstanceTo(defaultPeers);
        }

        state.applyAttributes(this.attributes);
        state.applyTo(this.guid);

        updateLastUsedGuidState();
        return this;
    }

    @Override
    public State.Options applyTo(Object... peers) {
        if (peers.length == 0 && defaultPeers.isEmpty()) {
            throw new IllegalArgumentException("Item without default peers must be applied with explicit peer list");
        }

        return applyToImpl(Preconditions.check(Preconditions::apply, peers));
    }

    private State.Options applyToImpl(Set<QualifiedString> flattenedPeers) {
        applyInstanceTo(defaultPeers);
        applyInstanceTo(flattenedPeers);

        StateImpl state = state();
        state.applyAttributes(this.attributes);
        state.applyTo(this.guid);
        state.applyTo(flattenedPeers);
        updateLastUsedGuidState();
        return this;
    }

    private void applyInstanceTo(Collection<QualifiedString> items) {
        for (QualifiedString peer : items) {
            state(peer).applyTo(this.guid);
            state(peer).applyTo(this.guid.kind());
        }
    }

    @Override
    public Persistence over(long duration, TimeUnit unit) {
        StateImpl state = state();
        state.over(duration, unit);
        return this;
    }

    @Override
    public Persistence over(Duration duration) {
        StateImpl state = state();
        state.over(duration);
        return this;
    }

    @Override
    public void remember(Until forget) {
        StateImpl state = state();
        state.remember(forget);
        state(forget).applyTo(this.guid).remember(forget);
    }

    public Collection<QualifiedString> attributesAndPeers() {
        Collection<QualifiedString> attributesAndPeers = new HashSet<>();
        attributesAndPeers.addAll(attributes);
        attributesAndPeers.addAll(defaultPeers);
        return attributesAndPeers;
    }

    @Override
    public void remove() {
        StateImpl state = state();
        if (containsMyGuid(state)) {
            for (Object peer : new ArrayList<>(state.peers())) {
                if (!QualifiedString.isItemGuid(peer)) {
                    StateImpl peerState = state(peer);
                    peerState.removeFrom(this.guid);
                    state.removeFrom(this.guid);
                }
            }

            // TODO better: remove only peers of this instances that are not peered by any other item

            releaseInstanceGuid();
            if (state.peers().isEmpty()) {
                state.remove();
            }
            updateLastUsedGuidState(duration());
        } else {
            throw new IllegalStateException("This item is not applied: " + guid);
        }
    }

    @Override
    public void removeFrom(Object... peers) {
        removeInternal(Preconditions.check(Preconditions::remove, peers));
    }

    private void removeInternal(Set<QualifiedString> peers) {
        StateImpl state = state();
        if (containsMyGuid(state)) {
            for (Object peer : peers) {
                if (!QualifiedString.isItemGuid(peer)) {
                    StateImpl peerState = state(peer);
                    peerState.removeFrom(this.guid);
                }
            }

            releaseInstanceGuid();
            if (state.peers().isEmpty()) {
                state.remove();
            }
            updateLastUsedGuidState(duration());
        } else {
            throw new IllegalStateException("This item is not applied: " + guid);
        }
    }

    public boolean releaseInstanceGuid() {
        StateImpl state = state();
        if (state.peerStates().stream().anyMatch(this::containsMyGuid)) {
            return false;
        }
        state.removeFrom(this.guid);
        return true;
    }

    private void updateLastUsedGuidState() {
        lastUsed().updateLastUsed();
    }

    private void updateLastUsedGuidState(Duration duration) {
        lastUsed().updateLastUsed(duration);
    }

    private StateImpl lastUsed() {
        var lastUsedStateName = ReflectionUtils.qualified(guid.kind().toString(), guid.guid().orElseThrow());
        return (StateImpl) teaseLib.state(domain, lastUsedStateName);
    }

    private Stream<StateImpl> defaultStates() {
        return defaultPeers.stream().map(this::state);
    }

    private StateImpl state(Object peer) {
        return (StateImpl) teaseLib.state(domain, peer);
    }

    private boolean containsMyGuid(StateImpl state) {
        return state.peers().contains(this.guid);
    }

    private boolean stateIsThis(StateImpl state) {
        return state.is(this.guid);
    }

    @Override
    public void applyAttributes(Object... attributes) {
        state().applyAttributes(attributes);
    }

    @Override
    public boolean removed() {
        return !applied();
    }

    @Override
    public long removed(TimeUnit unit) {
        if (applied()) {
            return 0;
        } else {
            return lastUsed().removed(unit);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
        result = prime * result + ((domain == null) ? 0 : domain.hashCode());
        result = prime * result + ((guid == null) ? 0 : guid.hashCode());
        result = prime * result + defaultPeers.hashCode();
        result = prime * result + ((teaseLib == null) ? 0 : teaseLib.hashCode());
        result = prime * result + ((available == null) ? 0 : available.hashCode());
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
        ItemImpl other = (ItemImpl) obj;
        if (attributes == null) {
            if (other.attributes != null)
                return false;
        } else if (!attributes.equals(other.attributes))
            return false;
        if (displayName == null) {
            if (other.displayName != null)
                return false;
        } else if (!displayName.equals(other.displayName))
            return false;
        if (domain == null) {
            if (other.domain != null)
                return false;
        } else if (!domain.equals(other.domain))
            return false;
        if (guid == null) {
            if (other.guid != null)
                return false;
        } else if (!guid.equals(other.guid))
            return false;
        if (!defaultPeers.equals(other.defaultPeers))
            return false;
        if (teaseLib == null) {
            if (other.teaseLib != null)
                return false;
        } else if (!teaseLib.equals(other.teaseLib))
            return false;
        if (available == null) {
            if (other.available != null)
                return false;
        } else if (!available.equals(other.available))
            return false;
        return true;
    }
}
