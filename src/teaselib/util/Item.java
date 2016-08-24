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

    public static String createDisplayName(Object item) {
        return item.toString().replace("_", " ");
    }

    public Item(T item, TeaseLib.PersistentBoolean value) {
        this.item = item;
        this.value = value;
        this.displayName = createDisplayName(item);
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
