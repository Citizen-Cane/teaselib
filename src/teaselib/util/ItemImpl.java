/**
 * 
 */
package teaselib.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import teaselib.Duration;
import teaselib.State;
import teaselib.core.StateMaps;
import teaselib.core.TeaseLib;
import teaselib.core.util.QualifiedItem;

/**
 * @author someone
 *
 */
public class ItemImpl implements Item, StateMaps.Attributes {

    final TeaseLib teaseLib;
    public final String domain;
    public final Object item;
    public final TeaseLib.PersistentBoolean value;
    public final String displayName;
    public final Object[] peers;
    public final Set<Object> attributes;

    public static String createDisplayName(Object item) {
        return item.toString().replace("_", " ");
    }

    public ItemImpl(TeaseLib teaseLib, String domain, Object item, TeaseLib.PersistentBoolean value) {
        this(teaseLib, domain, item, value, createDisplayName(item));
    }

    public ItemImpl(TeaseLib teaseLib, String domain, Object item, TeaseLib.PersistentBoolean value,
            String displayName) {
        this(teaseLib, domain, item, value, displayName, new Object[] {}, new Object[] {});
    }

    public ItemImpl(TeaseLib teaseLib, String domain, Object item, TeaseLib.PersistentBoolean value, String displayName,
            Object[] peers, Object[] attributes) {
        this.teaseLib = teaseLib;
        this.domain = domain;
        this.item = item;
        this.value = value;
        this.displayName = displayName;
        this.peers = peers;
        Set<Object> all = new HashSet<Object>();
        all.add(item);
        all.addAll(Arrays.asList(attributes));
        this.attributes = Collections.unmodifiableSet(all);
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
                return hasAllAttributes(this.attributes, attributes);
            }
        } else
            return false;
    }

    public static boolean hasAllAttributes(Set<Object> mine, Object[] others) {
        attributeLoop: for (Object value : others) {
            QualifiedItem<?> item = QualifiedItem.of(value);
            for (Object attribute : mine) {
                if (item.equals(attribute)) {
                    continue attributeLoop;
                }
            }
            return false;
        }
        return true;
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
        State state = teaseLib.state(domain, item);
        state.applyTo(peers);
        return applyTo();
    }

    @Override
    public <S extends Object> State.Options applyTo(S... items) {
        if (items.length == 0 && peers.length == 0) {
            throw new IllegalArgumentException("Item without default peers must be applied with explicit peer list");
        }
        State state = teaseLib.state(domain, item);
        Object[] array = new Object[attributes.size()];
        ((StateMaps.Attributes) state).applyAttributes(attributes.toArray(array));
        return state.applyTo(items);
    }

    @Override
    public Persistence remove() {
        return teaseLib.state(domain, item).remove();
    }

    @Override
    public <S extends Object> Persistence removeFrom(S... peer) {
        return teaseLib.state(domain, item).removeFrom(peer);
    }

    @Override
    public void applyAttributes(Object... attributes) {
        ((StateMaps.Attributes) teaseLib.state(domain, item)).applyAttributes(attributes);
    }

}
