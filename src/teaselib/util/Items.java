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
public class Items extends ArrayList<Item> {
    private static final long serialVersionUID = 1L;

    public static final Items None = new Items();

    public Items() {
        super();
    }

    public Items(Collection<? extends Item> items) {
        super(items);
    }

    public Items(int capacity) {
        super(capacity);
    }

    public boolean isAvailable() {
        return available().size() > 0;
    }

    public Items available() {
        Items available = new Items();
        for (Item item : this) {
            if (item.isAvailable()) {
                available.add(item);
            }
        }
        return available;
    }

    public <S> Item get(S... attributes) {
        for (Item item : this) {
            if (item.is(attributes)) {
                return item;
            }
        }
        return Item.NotAvailable;
    }

    public <S> Items all(S... attributes) {
        if (attributes.length == 0) {
            return this;
        } else {
            Items items = new Items();
            for (Item item : this) {
                if (item.is(attributes)) {
                    items.add(item);
                }
            }
            return items;
        }
    }

}
