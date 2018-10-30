/**
 * 
 */
package teaselib.util;

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
    public final String guid;
    public final Object item;
    public final String displayName;
    public final TeaseLib.PersistentBoolean value;
    public final Object[] defaultPeers;
    public final Set<Object> attributes;

    public static String createDisplayName(Object item) {
        return item.toString().replace("_", " ");
    }

    public ItemImpl(TeaseLib teaseLib, Object item, String domain, String guid, String displayName) {
        this(teaseLib, item, domain, guid, displayName, new Object[] {}, new Object[] {});
    }

    public ItemImpl(TeaseLib teaseLib, Object item, String domain, String guid, String displayName,
            Object[] defaultPeers, Object[] attributes) {
        this.teaseLib = teaseLib;
        this.item = item;
        this.domain = domain;
        this.guid = guid;
        this.displayName = displayName;
        this.value = teaseLib.new PersistentBoolean(domain, QualifiedItem.namespaceOf(item), guid);
        this.defaultPeers = defaultPeers;
        this.attributes = attributes(item, attributes);
    }

    public static ItemImpl restoreFromUserItems(TeaseLib teaseLib, String domain, Storage storage) {
        String item = storage.next();
        String guid = storage.next();
        return (ItemImpl) teaseLib.getByGuid(domain, item, guid);
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
        return guid + " " + attributes + " " + teaseLib.state(domain, item).toString();
    }

    boolean has(Object... attributes2) {
        Stream<Object> attributesAndPeers = Stream.concat(attributes.stream(), Arrays.stream(defaultPeers));
        return has(attributesAndPeers, attributes2);
    }

    private static boolean has(Stream<Object> available, Object... desired) {
        return available.filter(element -> contains(desired, QualifiedItem.of(element))).count() == desired.length;
    }

    static boolean contains(Object[] attributes2, QualifiedItem item) {
        for (Object object : attributes2) {
            if (QualifiedItem.of(object).equals(item)) {
                return true;
            }
        }
        return false;
    }

    @Override
    // TODO Try to reduce number of checks
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

        if (StateMaps.hasAllAttributes(((StateImpl) teaseLib.state(domain, item)).getAttributes(), attributes2))
            return applied();

        // TODO Replace by stricter check
        long count = peerCount(Arrays.stream(attributes2));
        if (count < attributes2.length)
            return false;

        return stateContainsAll(attributes2);
    }

    private boolean stateContainsAll(Object... attributes) {
        return teaseLib.state(domain, item).is(attributes);
    }

    public Set<Object> peers() {
        return new HashSet<>(Arrays.asList(defaultPeers));
    }

    @Override
    public boolean canApply() {
        for (Object peer : defaultPeers) {
            if (teaseLib.state(domain, peer).applied()) {
                return false;
            }
        }
        State state = teaseLib.state(domain, item);
        return !state.is(this);
    }

    @Override
    public boolean applied() {
        State state = teaseLib.state(domain, item);
        if (state.applied()) {
            if (defaultPeers.length > 0) {
                return appliedToPeers();
            } else {
                return ((StateImpl) state).peers().contains(guid);
            }
        } else {
            return false;
        }
    }

    private boolean appliedToPeers() {
        if (defaultPeers.length == 0) {
            return true;
        } else
            return peers().stream().filter(peer -> !(peer instanceof ActionState)).anyMatch(peer -> {
                return ((StateImpl) teaseLib.state(domain, peer)).peers().contains(this);
            });
    }

    private long peerCount(Stream<?> stream) {
        return stream.filter(peer -> !(peer instanceof ActionState))
                .filter(peer -> teaseLib.state(domain, peer).is(this)).count();
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
    public State.Options applyTo(Object... items) {
        if (items.length == 0 && defaultPeers.length == 0) {
            throw new IllegalArgumentException("Item without default peers must be applied with explicit peer list");
        }

        // TODO Implement a generic solution for processing arrays, lists, and Items class instances
        // (used by Rakhee key release setup)

        for (Object s : items) {
            if (s instanceof List || s instanceof Object[]) {
                throw new IllegalArgumentException("Applying lists and arrays isn't supported yet: " + s);
            }
        }

        applyInstanceTo(defaultPeers);
        applyInstanceTo(items);

        State state = teaseLib.state(domain, item);
        applyMyAttributesTo(state);
        state.applyTo(this.guid);
        return state.applyTo(items);
    }

    private void applyMyAttributesTo(State state) {
        Object[] array = new Object[attributes.size()];
        ((StateMaps.Attributes) state).applyAttributes(attributes.toArray(array));
    }

    private void applyInstanceTo(Object... items) {
        for (Object peer : items) {
            teaseLib.state(domain, peer).applyTo(this);
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
        StateImpl state = (StateImpl) teaseLib.state(domain, item);
        if (state.peers().contains(this.guid)) {
            HashSet<Object> relevantPeers = new HashSet<>(state.peers());
            relevantPeers.addAll(Arrays.asList(defaultPeers));
            relevantPeers.addAll(attributes);

            for (Object peer : relevantPeers) {
                StateImpl peerState = (StateImpl) teaseLib.state(domain, peer);
                long instancesOfSameKind = peerState.instancesOfSameKind(this);

                // Some tests assert that removing a similar item also works (gates of hell vs chastity belt)
                // - this is implicitly resolved by removing the state completely on removing the last item instance
                if (peerState.anyMoreItemInstanceOfSameKind(this)) {
                    peerState.removeFrom(this);
                    if (peerState.anyMoreItemInstanceOfSameKind(this)) {
                        if (instancesOfSameKind > 1 && instancesOfSameKind > peerState.instancesOfSameKind(this)) {
                            // Gross hack to remove just the item instance
                            // TODO Remove all applied attributes that belongs to the item
                            // TODO Make this work with items of same kind but different default peers
                            return;
                        }
                    }
                }
            }

            releaseInstanceGuid();
            state.remove();
        }
    }

    @Override
    public void removeFrom(Object... peers) {
        StateImpl state = (StateImpl) teaseLib.state(domain, item);
        if (state.peers().contains(this.guid)) {
            for (Object peer : peers) {
                StateImpl remove = (StateImpl) teaseLib.state(domain, peer);
                remove.removeFrom(this);
                if (!state.anyMoreItemInstanceOfSameKind(this.value)) {
                    remove.removeFrom(peer);
                }
            }
        }
    }

    public void releaseInstanceGuid() {
        for (Object peer : defaultPeers) {
            StateImpl peerState = (StateImpl) teaseLib.state(domain, peer);
            if (peerState.peers().contains(this)) {
                return;
            }
        }
        StateImpl state = (StateImpl) teaseLib.state(domain, item);
        state.removeFrom(this.guid);
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
