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
import teaselib.core.devices.ActionState;
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
    public final Object item;
    public final String displayName;
    public final TeaseLib.PersistentBoolean value;
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
        this.item = item;
        this.domain = domain;
        this.guid = guid;
        this.displayName = displayName;
        this.value = teaseLib.new PersistentBoolean(domain, QualifiedItem.namespaceOf(item), guid.name());
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
        return Arrays.asList(Persist.persist(QualifiedItem.of(item).toString()), Persist.persist(guid));
    }

    private static Set<Object> attributes(Object item, Object[] attributes) {
        Set<Object> all = new HashSet<>();
        all.add(item);
        all.addAll(Arrays.asList(attributes));
        return Collections.unmodifiableSet(all);
    }

    @Override
    public boolean isAvailable() {
        return value.value();
    }

    @Override
    public void setAvailable(boolean isAvailable) {
        value.set(isAvailable);
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return guid.name() + " " + attributes + " " + teaseLib.state(domain, item).toString();
    }

    boolean has(Object... attributes2) {
        Stream<Object> attributesAndPeers = Stream.concat(attributes.stream(), Arrays.stream(defaultPeers));
        return has(attributesAndPeers, attributes2);
    }

    private static boolean has(Stream<Object> available, Object... desired) {
        return available.filter(element -> contains(desired, QualifiedItem.of(element))).count() == desired.length;
    }

    static boolean contains(Object[] attributes2, QualifiedItem item) {
        return Arrays.stream(attributes2).map(QualifiedItem::of).anyMatch(i -> i.equals(item));
    }

    @Override
    public boolean is(Object... attributes3) {
        // TODO fails release action test - proxy removed from action state
        // -> wrong test afterwards - this slipped through
        Object[] attributes2 = attributes3; // AbstractProxy.removeProxies(attributes3);
        if (attributes2.length == 0) {
            return false;
        } else if (attributes2.length == 1 && attributes2[0] instanceof Item) {
            return AbstractProxy.removeProxy(attributes2[0]) == this;
        } else if (has(this.attributes.stream(), attributes2))
            return true;

        if (StateMaps.hasAllAttributes((state(item)).getAttributes(), attributes2))
            return applied();

        // TODO Replace by stricter check
        long count = peerCount(Arrays.stream(attributes2));
        if (count < attributes2.length)
            return false;

        return stateContainsAll(attributes2);
    }

    private boolean stateContainsAll(Object... attributes) {
        return state(item).is(attributes);
    }

    public Set<Object> peers() {
        return new HashSet<>(Arrays.asList(defaultPeers));
    }

    @Override
    public boolean canApply() {
        if (defaultPeers.length > 0) {
            return Arrays.stream(defaultPeers).map(this::state).allMatch(state -> !state.applied());
        } else {
            return !state(item).is(this);
        }
    }

    @Override
    public boolean applied() {
        StateImpl state = state(item);
        if (state.applied()) {
            if (defaultPeers.length > 0) {
                return appliedToPeers();
            } else {
                return containsMyGuid(state);
            }
        } else {
            return false;
        }
    }

    private boolean appliedToPeers() {
        if (defaultPeers.length == 0) {
            return true;
        } else
            return peers().stream().filter(ActionState::isntActionState).map(this::state).anyMatch(this::containsMe);
    }

    private long peerCount(Stream<?> stream) {
        return stream.filter(ActionState::isntActionState).map(this::state).filter(this::stateIsThis).count();
    }

    @Override
    public boolean expired() {
        return teaseLib.state(domain, item).expired();
    }

    @Override
    public Duration duration() {
        return teaseLib.state(domain, item).duration();
    }

    @Override
    public State.Options apply() {
        State state = teaseLib.state(domain, item);

        if (defaultPeers.length == 0) {
            state.apply();
        } else {
            applyInstanceTo(defaultPeers);
        }

        state.applyTo(this.guid);
        applyMyAttributesTo(state);
        return (State.Options) state;
    }

    @Override
    public State.Options applyTo(Object... peers) {
        if (peers.length == 0 && defaultPeers.length == 0) {
            throw new IllegalArgumentException("Item without default peers must be applied with explicit peer list");
        }

        peers = StateMaps.flatten(peers);

        applyInstanceTo(defaultPeers);
        applyInstanceTo(peers);

        State state = teaseLib.state(domain, item);
        applyMyAttributesTo(state);
        state.applyTo(this.guid);
        return state.applyTo(peers);
    }

    private void applyMyAttributesTo(State state) {
        Object[] array = new Object[attributes.size()];
        ((StateMaps.Attributes) state).applyAttributes(attributes.toArray(array));
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
        StateImpl state = state(item);
        if (containsMyGuid(state)) {
            for (Object peer : new ArrayList<>(state.peers())) {
                if (!(peer instanceof ItemGuid)) {
                    StateImpl peerState = state(peer);
                    peerState.removeFrom(this);
                    state.removeFrom(this.guid);
                }
            }

            if (state.peers().stream().noneMatch(ItemGuid::isItemGuid) && releaseInstanceGuid()) {
                state.remove();
            }
        }
    }

    @Override
    public void removeFrom(Object... peers) {
        StateImpl state = state(item);
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
        StateImpl state = state(item);
        if (peersReferenceMe(state)) {
            return false;
        }

        if (state.peers().stream().filter(ItemGuid::isntItemGuid).map(this::state).anyMatch(this::containsMe)) {
            return false;
        }

        state.removeFrom(this.guid);
        return true;
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
        ((StateMaps.Attributes) teaseLib.state(domain, item)).applyAttributes(attributes);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
        result = prime * result + ((domain == null) ? 0 : domain.hashCode());
        result = prime * result + ((item == null) ? 0 : item.hashCode());
        result = prime * result + Arrays.hashCode(defaultPeers);
        result = prime * result + ((teaseLib == null) ? 0 : teaseLib.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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
        if (item == null) {
            if (other.item != null)
                return false;
        } else if (!item.equals(other.item))
            return false;
        if (!Arrays.equals(defaultPeers, other.defaultPeers))
            return false;
        if (teaseLib == null) {
            if (other.teaseLib != null)
                return false;
        } else if (!teaseLib.equals(other.teaseLib))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }
}
