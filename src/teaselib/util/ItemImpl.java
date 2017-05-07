/**
 * 
 */
package teaselib.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import teaselib.State;
import teaselib.core.TeaseLib;
import teaselib.core.util.QualifiedItem;

/**
 * @author someone
 *
 */
public class ItemImpl implements Item {

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

    public ItemImpl(TeaseLib teaseLib, String domain, Object item,
            TeaseLib.PersistentBoolean value) {
        this(teaseLib, domain, item, value, createDisplayName(item));
    }

    public ItemImpl(TeaseLib teaseLib, String domain, Object item, TeaseLib.PersistentBoolean value,
            String displayName) {
        this(teaseLib, domain, item, value, displayName, new Object[] {}, new Object[] {});
    }

    public ItemImpl(TeaseLib teaseLib, String domain, Object item, TeaseLib.PersistentBoolean value,
            String displayName, Object[] peers, Object[] attributes) {
        this.teaseLib = teaseLib;
        this.domain = domain;
        this.item = item;
        this.value = value;
        this.displayName = displayName;
        this.peers = peers;
        this.attributes = new HashSet<Object>();
        this.attributes.add(item);
        this.attributes.addAll(Arrays.asList(attributes));
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
        return value.name;
    }

    @Override
    public <S> boolean is(S... attributes) {
        if (attributes.length > 0) {
            if (stateContainsAll(attributes)) {
                return true;
            } else {
                nextAttribute: for (S value : attributes) {
                    QualifiedItem<?> item = QualifiedItem.fromType(value);
                    for (Object attribute : this.attributes) {
                        if (item.equals(attribute)) {
                            continue nextAttribute;
                        }
                    }
                    return false;
                }
                return true;
            }
        } else
            return false;
    }

    private boolean stateContainsAll(Object... attributes) {
        if (!applied())
            return false;
        for (Object object : attributes) {
            return teaseLib.state(domain, item).is(object);
        }
        return true;
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

    private boolean applied() {
        return teaseLib.state(domain, item).applied();
    }

    @Override
    public State.Options apply() {
        return to();
    }

    @Override
    public <S extends Object> State.Options to(S... items) {
        State state = teaseLib.state(domain, item);
        state.apply(items);
        state.apply(peers);
        Object[] array = new Object[attributes.size()];
        return state.apply(attributes.toArray(array));
    }

    @Override
    public State remove() {
        return teaseLib.state(domain, item).remove();
    }

}
