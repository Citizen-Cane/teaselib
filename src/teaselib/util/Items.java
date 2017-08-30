/**
 * 
 */
package teaselib.util;

import java.util.ArrayList;
import java.util.Collection;

import teaselib.core.state.ItemProxy;

/**
 * @author Citizen-Cane
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
     * @return First item
     */
    public Item get() {
        return getInternal();
    }

    /**
     * Get a suitable item matching all attributes
     * 
     * @param attributes
     * @return An item that matches all attributes, or the first available, or {@link Item#NotAvailable}.
     */
    public <S extends Item.Attribute> Item get(S... attributes) {
        return getInternal(attributes);
    }

    public Item get(String... attributes) {
        return getInternal(attributes);
    }

    /**
     * Get first matching
     * 
     * @return
     */
    private <S> Item getInternal(S... attributes) {
        if (attributes.length == 0) {
            return firstAvailableOrNotAvailable();
        } else {
            for (Item item : this) {
                if (item instanceof ItemProxy) {
                    if (((ItemImpl) ((ItemProxy) item).item).has(attributes)) {
                        return item;
                    }
                } else if (((ItemImpl) item).has(attributes)) {
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

    public Items all() {
        return allInternal();
    }

    public <S extends Item.Attribute> Items all(S... attributes) {
        return allInternal(attributes);
    }

    public Items all(String... attributes) {
        return allInternal(attributes);
    }

    public <S> Items allInternal(S... attributes) {
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

    /**
     * Get matching or available item.
     * 
     * @param attributes
     * @return Prefer item matching attributes, but fall back to any other available
     */
    public <S> Item prefer(S... attributes) {
        Items items = allInternal(attributes);
        if (!items.isAvailable()) {
            items = allInternal().available();
        }
        return items.firstAvailableOrNotAvailable();
    }
}
