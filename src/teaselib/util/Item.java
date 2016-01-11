/**
 * 
 */
package teaselib.util;

import teaselib.TeaseLib;

/**
 * @author someone
 *
 */
public class Item<T> {

    public final T item;
    public final TeaseLib.PersistentBoolean value;
    public final String displayName;

    public static String createDisplayName(Object name) {
        String displayName = name.toString().replace("_", " ");
        return displayName;
    }

    public Item(T item, TeaseLib.PersistentBoolean value, String displayName) {
        this.item = item;
        this.value = value;
        this.displayName = displayName;
    }

    public boolean isAvailable() {
        return value.get();
    }

    public void setAvailable(boolean isAvailable) {
        value.set(isAvailable);
    }

    @Override
    public String toString() {
        return value.name;
    }
}
