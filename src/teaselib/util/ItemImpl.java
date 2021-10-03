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
import teaselib.core.StateMaps;
import teaselib.core.TeaseLib;
import teaselib.core.state.AbstractProxy;
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
    public final ItemGuid guid;
    public final String displayName;
    private final TeaseLib.PersistentBoolean available;
    public final List<Object> defaultPeers;
    public final Set<QualifiedString> attributes;

    public static String createDisplayName(Object item) {
        return item.toString().replace("_", " ");
    }

    public ItemImpl(TeaseLib teaseLib, String domain, ItemGuid guid, String displayName) {
        this(teaseLib, domain, guid, displayName, new Object[] {}, new Object[] {});
    }

    public ItemImpl(TeaseLib teaseLib, String domain, ItemGuid guid, String displayName, Object[] defaultPeers,
            Object[] attributes) {
        this.teaseLib = teaseLib;
        this.domain = domain;
        this.guid = guid;
        this.displayName = displayName;
        this.available = teaseLib.new PersistentBoolean(domain, value().toString(), guid.name() + "." + Available);
        this.defaultPeers = Collections.unmodifiableList(StateImpl.mapToQualifiedString(Arrays.asList(defaultPeers)));
        this.attributes = Collections.unmodifiableSet(
                StateImpl.mapToQualifiedStringTyped(attributes(guid.kind(), Arrays.asList(attributes))));
    }

    public QualifiedString value() {
        return guid.kind();
    }

    private StateImpl state() {
        return state(value());
    }

    public static ItemImpl restoreFromUserItems(TeaseLib teaseLib, String domain, Storage storage)
            throws ReflectiveOperationException {
        var guid = new ItemGuid(storage);
        return (ItemImpl) teaseLib.getItem(domain, guid.kind(), guid.name());
    }

    @Override
    public List<String> persisted() {
        return guid.persisted();
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
        return guid.name() + " " + attributes + " " + teaseLib.state(domain, value()).toString();
    }

    boolean has(List<? extends Object> desired) {
        Stream<Object> attributesAndPeers = Stream.concat(attributes.stream(), defaultPeers.stream());
        return has(attributesAndPeers, desired);
    }

    private static boolean has(Stream<? extends Object> available, List<? extends Object> desired) {
        return available.filter(element -> contains(desired, QualifiedString.of(element))).count() == desired.size();
    }

    static boolean contains(List<? extends Object> attributes2, QualifiedString item) {
        return attributes2.stream().anyMatch(item::equals);
    }

    @Override
    public boolean is(Object... attributes3) {
        List<Object> flattenedAttributes = StateImpl
                .mapToQualifiedString(StateMaps.flatten(AbstractProxy.removeProxies(attributes3)));
        return is(flattenedAttributes);
    }

    private boolean is(List<Object> flattenedAttributes) {
        if (flattenedAttributes.isEmpty()) {
            return false;
        } else if (flattenedAttributes.size() == 1 && flattenedAttributes.get(0) instanceof Item) {
            // TODO Remove these special cases:
            // attributes2[0] == this -> ItemIdentityTest.testThatItemIsNotOtherItem
            // state(value).is(attributes2[0]) -> ItemsTest.testItemAppliedToItems
            return flattenedAttributes.get(0) == this || state().is(flattenedAttributes.get(0));
        } else if (has(this.attributes.stream(), flattenedAttributes))
            return true;
        else {
            if (StateMaps.hasAllAttributes((state()).getAttributes(), flattenedAttributes)) {
                return applied();
            } else if (state(this).appliedToClassValues(attributes, flattenedAttributes)) {
                return true;
            } else if (state(this).appliedToClassState(state(this).peers(), flattenedAttributes)) {
                return true;
            } else if (!stateAppliesToMe(flattenedAttributes)) {
                return false;
            } else {
                return stateContainsAll(flattenedAttributes);
            }
        }
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
            return !state().is(this);
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
                return defaultStates().anyMatch(this::containsMe);
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

        state.applyTo(this.guid);
        state.applyAttributes(this.attributes);
        updateLastUsedGuidState();
        return this;
    }

    @Override
    public State.Options applyTo(Object... peers) {
        if (peers.length == 0 && defaultPeers.isEmpty()) {
            throw new IllegalArgumentException("Item without default peers must be applied with explicit peer list");
        }

        return applyToFlattenedPeers(StateImpl.mapToQualifiedString(StateMaps.flatten(peers)));
    }

    private State.Options applyToFlattenedPeers(Collection<Object> flattenedPeers) {
        applyInstanceTo(defaultPeers);
        applyInstanceTo(flattenedPeers);

        StateImpl state = state();
        state.applyAttributes(this.attributes);
        state.applyTo(this.guid);
        state.applyTo(flattenedPeers);
        updateLastUsedGuidState();
        return this;
    }

    private void applyInstanceTo(Collection<Object> items) {
        for (Object peer : items) {
            state(peer).applyTo(this);
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
        state(forget).applyTo(this).remember(forget);
    }

    public Collection<Object> attributesAndPeers() {
        Collection<Object> attributesAndPeers = new HashSet<>();
        attributesAndPeers.addAll(attributes);
        attributesAndPeers.addAll(defaultPeers);
        return attributesAndPeers;
    }

    @Override
    public void remove() {
        StateImpl state = state();
        if (containsMyGuid(state)) {
            for (Object peer : new ArrayList<>(state.peers())) {
                if (!(peer instanceof ItemGuid)) {
                    StateImpl peerState = state(peer);
                    peerState.removeFrom(this);
                    state.removeFrom(this.guid);
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

    @Override
    public void removeFrom(Object... peers) {
        removeInternal(StateImpl.mapToQualifiedString(StateMaps.flatten(AbstractProxy.removeProxies(peers))));
    }

    private void removeInternal(List<Object> peers) {
        StateImpl state = state();
        if (containsMyGuid(state)) {
            for (Object peer : peers) {
                if (!(peer instanceof ItemGuid)) {
                    StateImpl peerState = state(peer);
                    peerState.removeFrom(this);
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
        if (peersReferenceMe(state)) {
            return false;
        }

        if (state.peerStates().stream().anyMatch(this::containsMe)) {
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
        var lastUsedStateName = ReflectionUtils.qualified(guid.kind().toString(), guid.name());
        return (StateImpl) teaseLib.state(domain, lastUsedStateName);
    }

    private Stream<StateImpl> defaultStates() {
        return defaultPeers.stream().map(this::state);
    }

    private StateImpl state(Object peer) {
        return (StateImpl) teaseLib.state(domain, peer);
    }

    private boolean containsMe(StateImpl state) {
        return state.peers().contains(this);
    }

    private boolean containsMyGuid(StateImpl state) {
        return state.peers().contains(this.guid);
    }

    private static boolean isItemImpl(Object peer) {
        return peer instanceof ItemImpl;
    }

    private boolean stateIsThis(StateImpl state) {
        return state.is(this);
    }

    private static boolean peersReferenceMe(StateImpl state) {
        Set<Object> peers = state.peers();
        return peers.stream().filter(ItemImpl::isItemImpl).map(ItemImpl.class::cast).map(itemImpl -> itemImpl.guid)
                .filter(peers::contains).count() > 0;
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
