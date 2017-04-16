/**
 * 
 */
package teaselib.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import teaselib.core.TeaseLib;

/**
 * @author someone
 *
 */
public class ItemImpl implements Item {

    public final Object item;
    public final TeaseLib.PersistentBoolean value;
    public final String displayName;
    public final Set<Object> attributes;

    public static String createDisplayName(Object item) {
        return item.toString().replace("_", " ");
    }

    public ItemImpl(Object item, TeaseLib.PersistentBoolean value) {
        this(item, value, createDisplayName(item));
    }

    public ItemImpl(Object item, TeaseLib.PersistentBoolean value, String displayName) {
        this(item, value, displayName, new Enum<?>[] {});
    }

    public ItemImpl(Object item, TeaseLib.PersistentBoolean value, String displayName,
            Enum<?>... attributes) {
        this.item = item;
        this.value = value;
        this.displayName = displayName;
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
            return this.attributes.containsAll(Arrays.asList(attributes));
        } else
            return false;
    }
}
