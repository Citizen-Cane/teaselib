/**
 * 
 */
package teaselib.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import teaselib.Duration;
import teaselib.State;
import teaselib.core.StateMaps;
import teaselib.core.StateMaps.StateImpl;
import teaselib.core.TeaseLib;
import teaselib.core.util.Persist;
import teaselib.core.util.Persist.Persistable;
import teaselib.core.util.QualifiedItem;

/**
 * @author someone
 *
 */
public class ItemImpl implements Item, StateMaps.Attributes, Persistable {
    final TeaseLib teaseLib;
    public final String domain;
    public final String namespace;
    public final String name;
    public final Object item;
    public final TeaseLib.PersistentBoolean value;
    public final String displayName;
    public final Object[] peers;
    public final Set<Object> attributes;

    public static String createDisplayName(Object item) {
        return item.toString().replace("_", " ");
    }

    public ItemImpl(TeaseLib teaseLib, Object item, String domain, String namespace, String name, String displayName) {
        this(teaseLib, item, domain, namespace, name, displayName, new Object[] {}, new Object[] {});
    }

    public ItemImpl(TeaseLib teaseLib, Object item, String domain, String namespace, String name, String displayName,
            Object[] peers, Object[] attributes) {
        this.teaseLib = teaseLib;
        this.item = item;
        this.domain = domain;
        this.namespace = namespace;
        this.name = name;
        this.displayName = displayName;
        this.value = teaseLib.new PersistentBoolean(domain, namespace, name);
        this.peers = peers;
        this.attributes = attributes(item, attributes);
    }

    @Override
    public List<String> persisted() {
        return Arrays.asList(Persist.persist(item), domain, namespace, name, displayName, Persist.persist(peers),
                Persist.persist(attributes));
    }

    public ItemImpl(TeaseLib teaseLib, Persist.Storage persisted) {
        this.teaseLib = teaseLib;
        this.item = persisted.next();
        this.domain = persisted.next();
        this.namespace = persisted.next();
        this.name = persisted.next();
        this.displayName = persisted.next();
        this.value = teaseLib.new PersistentBoolean(domain, namespace, name);
        this.peers = persisted.next();
        this.attributes = persisted.next();
    }

    @Deprecated
    public ItemImpl(TeaseLib teaseLib, String domain, Object item, TeaseLib.PersistentBoolean value) {
        this(teaseLib, domain, item, value, createDisplayName(item));
    }

    @Deprecated
    public ItemImpl(TeaseLib teaseLib, String domain, Object item, TeaseLib.PersistentBoolean value,
            String displayName) {
        this(teaseLib, domain, item, value, displayName, new Object[] {}, new Object[] {});
    }

    @Deprecated
    public ItemImpl(TeaseLib teaseLib, String domain, Object item, TeaseLib.PersistentBoolean value, String displayName,
            Object[] peers, Object[] attributes) {
        this.teaseLib = teaseLib;
        this.domain = domain;
        this.namespace = QualifiedItem.of(item).namespace();
        this.name = value.name;
        this.item = item;
        this.value = value;
        this.displayName = displayName;
        this.peers = peers;
        this.attributes = attributes(item, attributes);
    }

    private static Set<Object> attributes(Object item, Object[] attributes) {
        Set<Object> all = new HashSet<Object>();
        all.add(item);
        all.addAll(Arrays.asList(attributes));
        Set<Object> unmodifiableSet = Collections.unmodifiableSet(all);
        return unmodifiableSet;
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
        return value.name + " " + attributes + " " + teaseLib.state(domain, value).toString();
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

    public boolean has(Object... attributes) {
        return StateMaps.hasAllAttributes(this.attributes, attributes);
    }

    private boolean stateContainsAll(Object... attributes) {
        if (!applied()) {
            return false;
        } else {
            return teaseLib.state(domain, item).is(attributes);
        }
    }

    public Set<Object> peers() {
        return new HashSet<Object>(Arrays.asList(peers));
    }

    @Override
    public boolean canApply() {
        for (Object peer : peers) {
            if (teaseLib.state(domain, peer).applied()) {
                return false;
            }
        }
        return true;
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
        applyInstanceTo(peers);

        State state = teaseLib.state(domain, item);
        state.applyTo(peers);
        return applyTo();
    }

    @Override
    public <S extends Object> State.Options applyTo(S... items) {
        if (items.length == 0 && peers.length == 0) {
            throw new IllegalArgumentException("Item without default peers must be applied with explicit peer list");
        }

        applyInstanceTo(items);

        State state = teaseLib.state(domain, item);

        Object[] array = new Object[attributes.size()];
        ((StateMaps.Attributes) state).applyAttributes(attributes.toArray(array));
        return state.applyTo(items);
    }

    private void applyInstanceTo(Object... items) {
        for (Object peer : items) {
            teaseLib.state(domain, peer).applyTo(this);
        }
    }

    @Override
    public State.Persistence remove() {
        StateImpl state = (StateImpl) teaseLib.state(domain, item);
        for (Object peer : state.peers()) {
            teaseLib.state(domain, peer).removeFrom(this);
        }

        for (Object peer : new HashSet<Object>(state.peers())) {
            teaseLib.state(domain, peer).removeFrom(this.item);
        }

        return state.remove();
    }

    @Override
    public <S extends Object> State.Persistence removeFrom(S... peer) {
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
        result = prime * result + Arrays.hashCode(peers);
        result = prime * result + ((teaseLib == null) ? 0 : teaseLib.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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
        if (!Arrays.equals(peers, other.peers))
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
