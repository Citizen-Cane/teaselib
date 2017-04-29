/**
 * 
 */
package teaselib.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import teaselib.State;
import teaselib.core.TeaseLib;

/**
 * @author someone
 *
 */
public class ItemImpl implements Item {

    final TeaseLib teaseLib;
    public final Object item;
    public final TeaseLib.PersistentBoolean value;
    public final String displayName;
    public final Enum<?>[] peers;
    public final Set<Object> attributes;

    public static String createDisplayName(Object item) {
        return item.toString().replace("_", " ");
    }

    public ItemImpl(TeaseLib teaseLib, Object item, TeaseLib.PersistentBoolean value) {
        this(teaseLib, item, value, createDisplayName(item));
    }

    public ItemImpl(TeaseLib teaseLib, Object item, TeaseLib.PersistentBoolean value,
            String displayName) {
        this(teaseLib, item, value, displayName, new Enum<?>[] {}, new Enum<?>[] {});
    }

    public ItemImpl(TeaseLib teaseLib, Object item, TeaseLib.PersistentBoolean value,
            String displayName, Enum<?>[] peers, Enum<?>[] attributes) {
        this.teaseLib = teaseLib;
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
                List<S> attributeList = Arrays.asList(attributes);
                return this.attributes.containsAll(attributeList);
            }
        } else
            return false;
    }

    private boolean stateContainsAll(Object... attributes) {
        if (!applied())
            return false;
        for (Object object : attributes) {
            return applied() && state().is(object);
        }
        return true;
    }

    public Set<Enum<?>> peers() {
        return new HashSet<Enum<?>>(Arrays.asList(peers));
    }

    @Override
    public boolean canApply() {
        for (Enum<?> item : peers) {
            if (teaseLib.state(item).applied()) {
                return false;
            }
        }
        return true;
    }

    private boolean applied() {
        if (itemIsCompatibleToStateImplementation()) {
            return state().applied();
        } else {
            return false;
        }
    }

    @Override
    public State.Options apply() {
        return to();
    }

    @Override
    public <S extends Enum<?>> State.Options to(S... items) {
        State state = state();
        state.apply(items);
        state.apply(peers);
        return state.apply(allEnumsOf(attributes));
    }

    private static Enum<?>[] allEnumsOf(Set<?> attributes) {
        Set<Enum<?>> allEnums = new HashSet<Enum<?>>();
        for (Object object : attributes) {
            if (object instanceof Enum<?>) {
                allEnums.add((Enum<?>) object);
            }
        }
        Enum<?>[] array = new Enum<?>[allEnums.size()];
        return allEnums.toArray(array);
    }

    @Override
    public State remove() {
        return state().remove();
    }

    private State state() {
        if (itemIsCompatibleToStateImplementation()) {
            return teaseLib.state((Enum<?>) item);
        } else {
            throw new UnsupportedOperationException("State does only support enums");
        }
    }

    private boolean itemIsCompatibleToStateImplementation() {
        return item instanceof Enum<?>;
    }

}
