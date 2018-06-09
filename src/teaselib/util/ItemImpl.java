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

import teaselib.Duration;
import teaselib.State;
import teaselib.core.StateImpl;
import teaselib.core.StateMaps;
import teaselib.core.TeaseLib;
import teaselib.core.state.AbstractProxy;
import teaselib.core.util.Persist;
import teaselib.core.util.Persist.Persistable;
import teaselib.core.util.QualifiedItem;

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

    public static ItemImpl restore(TeaseLib teaseLib, String domain, Persist.Storage storage) {
        String guid = storage.next();
        return (ItemImpl) teaseLib.item(domain, guid);
    }

    @Override
    public List<String> persisted() {
        return Arrays.asList(Persist.persist(guid));
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

    @Override
    public boolean is(Object... attributes) {
        if (attributes.length > 0) {
            if (stateContainsAll(attributes)) {
                return true;
            } else {
                return StateMaps.hasAllAttributes(this.attributes, attributes);
            }
        } else
            return false;
    }

    private boolean stateContainsAll(Object... attributes) {
        if (!applied()) {
            return false;
        } else {
            return teaseLib.state(domain, item).is(attributes);
        }
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
        return teaseLib.state(domain, item).applied();
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
        applyInstanceTo(defaultPeers);

        State state = teaseLib.state(domain, item);
        applyMyAttributesTo(state);
        return (State.Options) state;
    }

    @Override
    @SafeVarargs
    public final <S extends Object> State.Options applyTo(S... items) {
        if (items.length == 0 && defaultPeers.length == 0) {
            throw new IllegalArgumentException("Item without default peers must be applied with explicit peer list");
        }

        for (S s : items) {
            if (s instanceof List || s instanceof Object[]) {
                throw new IllegalArgumentException("Applying lists and arrays isn't supported yet: " + s);
            }
        }

        applyInstanceTo(defaultPeers);
        applyInstanceTo(items);

        State state = teaseLib.state(domain, item);
        state.applyTo(this);
        applyMyAttributesTo(state);

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
    public State.Persistence remove() {
        StateImpl state = (StateImpl) teaseLib.state(domain, item);

        HashSet<Object> relevantPeers = new HashSet<>(state.peers());
        relevantPeers.addAll(Arrays.asList(defaultPeers));
        relevantPeers.addAll(attributes);

        for (Object peer : relevantPeers) {
            State peerState = teaseLib.state(domain, peer);
            peerState.removeFrom(this);
            peerState.removeFrom(this.item);
        }

        return state.remove();
    }

    @Override
    @SafeVarargs
    public final <S extends Object> State.Persistence removeFrom(S... peer) {
        return teaseLib.state(domain, item).removeFrom(peer);
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
