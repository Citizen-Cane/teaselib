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

    /**
     * Get a suitable item matching all attributes
     * 
     * @param attributes
     * @return An item that matches all attributes, or the first available, or
     *         {@link Item#NotAvailable}.
     */
    public <S> Item get(S... attributes) {
        if (attributes.length == 0) {
            return firstAvailableOrNotAvailable();
        } else {
            for (Item item : this) {
                if (item.is(attributes)) {
                    return item;
                }
            }
            return Item.NotAvailable;
        }
    }

    private Item firstAvailableOrNotAvailable() {
        Items available = available();
        if (available.size() > 0) {
            return available.get(0);
        } else {
            return Item.NotAvailable;
        }
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

    public <S> Item like(S... attributes) {
        if (attributes.length == 0) {
            return Item.NotAvailable;
        } else {
            for (Item item : this) {
                if (item.isAvailable() && item.is(attributes)) {
                    return item;
                }
            }
            return firstAvailableOrNotAvailable();
        }
    }
}
