/**
 * 
 */
package teaselib.util;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author someone
 *
 */
public class Items<T> extends ArrayList<Item<T>> {
    private static final long serialVersionUID = 1L;

    public Items() {
        super();
    }

    public Items(Collection<? extends Item<T>> items) {
        super(items);
    }

    public Items(int capacity) {
        super(capacity);
    }

    public Items<T> available() {
        Items<T> available = new Items<T>();
        for (Item<T> item : this) {
            if (item.isAvailable()) {
                available.add(item);
            }
        }
        return available;
    }
}
