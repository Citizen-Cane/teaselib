package teaselib.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import teaselib.Duration;
import teaselib.State;
import teaselib.core.StateImpl;
import teaselib.core.StateMaps;
import teaselib.core.TeaseLib;
import teaselib.core.state.AbstractProxy;
import teaselib.core.util.Persist;
import teaselib.core.util.Persist.Persistable;
import teaselib.core.util.QualifiedItem;
import teaselib.core.util.Storage;

/**
 * @author Citizen-Cane
 *
 */
public class ItemImpl implements Item, StateMaps.Attributes, Persistable {
    final TeaseLib teaseLib;

    public final String domain;
    public final ItemGuid guid;
    public final Object value;
    public final String displayName;
    private final TeaseLib.PersistentBoolean available;
    public final Object[] defaultPeers;
    public final Set<Object> attributes;

    public static String createDisplayName(Object item) {
        return item.toString().replace("_", " ");
    }

    public ItemImpl(TeaseLib teaseLib, Object item, String domain, ItemGuid guid, String displayName) {
        this(teaseLib, item, domain, guid, displayName, new Object[] {}, new Object[] {});
    }

    public ItemImpl(TeaseLib teaseLib, Object item, String domain, ItemGuid guid, String displayName,
            Object[] defaultPeers, Object[] attributes) {
        this.teaseLib = teaseLib;
        this.value = item;
        this.domain = domain;
        this.guid = guid;
        this.displayName = displayName;
        this.available = teaseLib.new PersistentBoolean(domain, QualifiedItem.namespaceOf(item), guid.name());
        this.defaultPeers = defaultPeers;
        this.attributes = attributes(item, attributes);
    }

    public static ItemImpl restoreFromUserItems(TeaseLib teaseLib, String domain, Storage storage) {
        String item = storage.next();
        ItemGuid guid = storage.next();
        return (ItemImpl) teaseLib.getByGuid(domain, item, guid.name());
    }

    @Override
    public List<String> persisted() {
        return Arrays.asList(Persist.persist(QualifiedItem.of(value).toString()), Persist.persist(guid));
    }

    private static Set<Object> attributes(Object item, Object[] attributes) {
        Set<Object> all = new HashSet<>();
        all.add(item);
        all.addAll(Arrays.asList(attributes));
        return Collections.unmodifiableSet(all);
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
        return guid.name() + " " + attributes + " " + teaseLib.state(domain, value).toString();
    }

    boolean has(Object... desired) {
        Stream<Object> attributesAndPeers = Stream.concat(attributes.stream(), Arrays.stream(defaultPeers));
        return has(attributesAndPeers, desired);
    }

    private static boolean has(Stream<Object> available, Object... desired) {
        return available.filter(element -> contains(desired, QualifiedItem.of(element))).count() == desired.length;
    }

    static boolean contains(Object[] attributes2, QualifiedItem item) {
        return Arrays.stream(attributes2).map(QualifiedItem::of).anyMatch(i -> i.equals(item));
    }

    @Override
    // TODO Doesn't work on mixed queries item.is(class, value, state);
    public boolean is(Object... attributes3) {
        Object[] attributes2 = AbstractProxy.removeProxies(attributes3);
        if (attributes2.length == 0) {
            return false;
        } else if (attributes2.length == 1 && attributes2[0] instanceof Item) {
            // TODO Remove these special cases:
            // attributes2[0] == this -> ItemIdentityTest.testThatItemIsNotOtherItem
            // state(value).is(attributes2[0]) -> ItemsTest.testItemAppliedToItems
            return attributes2[0] == this || state(value).is(attributes2[0]);
        } else if (has(this.attributes.stream(), attributes2))
            return true;
        else {
            if (StateMaps.hasAllAttributes((state(value)).getAttributes(), attributes2)) {
                return applied();
            } else if (state(this).appliedToClassValues(attributes, attributes2)) {
                return true;
            } else if (state(this).appliedToClassState(state(this).peers(), attributes2)) {
                return true;
            } else if (!stateAppliesToMe(attributes2)) {
                return false;
            } else {
                return stateContainsAll(attributes2);
            }
        }
    }

    private boolean stateAppliesToMe(Object[] attributes2) {
        return Arrays.stream(StateMaps.flatten(attributes2)).map(this::state).allMatch(this::stateIsThis);
    }

    private boolean stateContainsAll(Object... attributes) {
        return state(value).is(attributes);
    }

    @Override
    public boolean canApply() {
        if (defaultPeers.length > 0) {
            return defaultStates().allMatch(state -> !state.applied());
        } else {
            return !state(value).is(this);
        }
    }

    @Override
    public boolean applied() {
        StateImpl state = state(value);
        if (state.applied()) {
            if (defaultPeers.length > 0) {
                return defaultStates().anyMatch(this::containsMe);
            } else {
                return containsMyGuid(state);
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean expired() {
        return state(value).expired();
    }

    @Override
    public Duration duration() {
        return state(value).duration();
    }

    @Override
    public State.Options apply() {
        StateImpl state = state(value);

        if (defaultPeers.length == 0) {
            state.apply();
        } else {
            applyInstanceTo(defaultPeers);
        }

        state.applyTo(this.guid);
        applyMyAttributesTo(state);
        return state;
    }

    @Override
    public State.Options applyTo(Object... peers) {
        if (peers.length == 0 && defaultPeers.length == 0) {
            throw new IllegalArgumentException("Item without default peers must be applied with explicit peer list");
        }

        peers = StateMaps.flatten(peers);

        applyInstanceTo(defaultPeers);
        applyInstanceTo(peers);

        StateImpl state = state(value);
        applyMyAttributesTo(state);
        state.applyTo(this.guid);
        return state.applyTo(peers);
    }

    private void applyMyAttributesTo(StateImpl state) {
        Object[] array = new Object[attributes.size()];
        state.applyAttributes(attributes.toArray(array));
    }

    private void applyInstanceTo(Object... items) {
        for (Object peer : items) {
            state(peer).applyTo(this);
        }
    }

    public Collection<Object> attributesAndPeers() {
        Collection<Object> attributesAndPeers = new HashSet<>();
        attributesAndPeers.addAll(attributes);
        attributesAndPeers.addAll(Arrays.asList(defaultPeers));
        return attributesAndPeers;
    }

    @Override
    public void remove() {
        StateImpl state = state(value);
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
        }
    }

    @Override
    public void removeFrom(Object... peers) {
        StateImpl state = state(value);
        if (containsMyGuid(state)) {
            for (Object peer : peers) {
                if (!(peer instanceof ItemGuid)) {
                    StateImpl peerState = state(peer);
                    peerState.removeFrom(this);
                }
            }
        }
    }

    public boolean releaseInstanceGuid() {
        StateImpl state = state(value);
        if (peersReferenceMe(state)) {
            return false;
        }

        if (state.peerStates().anyMatch(this::containsMe)) {
            return false;
        }

        state.removeFrom(this.guid);
        return true;
    }

    private Stream<StateImpl> defaultStates() {
        return Arrays.stream(defaultPeers).map(this::state);
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
        return peers.stream().filter(ItemImpl::isItemImpl).map(peer -> (ItemImpl) peer).map(itemImpl -> itemImpl.guid)
                .filter(peers::contains).count() > 0;
    }

    @Override
    public void applyAttributes(Object... attributes) {
        state(value).applyAttributes(attributes);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
        result = prime * result + ((domain == null) ? 0 : domain.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        result = prime * result + Arrays.hashCode(defaultPeers);
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
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        if (!Arrays.equals(defaultPeers, other.defaultPeers))
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
