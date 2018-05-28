package teaselib.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import teaselib.core.state.ItemProxy;
import teaselib.core.util.QualifiedItem;

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

    public Stream<Item> filter(Predicate<? super Item> predicate) {
        return stream().filter(predicate);
    }

    public boolean anyAvailable() {
        return filter(Item::isAvailable).count() > 0;
    }

    public boolean allAvailable() {
        return filter(Item::isAvailable).count() == size();
    }

    public boolean anyAppliable() {
        return stream().filter(Item::canApply).count() > 0;
    }

    public boolean allAppliable() {
        return stream().filter(Item::canApply).count() == size();
    }

    public boolean anyApplied() {
        return stream().filter(Item::applied).count() > 0;
    }

    public boolean allApplied() {
        return stream().filter(Item::applied).count() == size();
    }

    public Items getAvailable() {
        return new Items(filter(Item::isAvailable).collect(Collectors.toList()));
    }

    public Items getApplyable() {
        return new Items(filter(Item::canApply).collect(Collectors.toList()));
    }

    public Items getApplied() {
        return new Items(filter(Item::applied).collect(Collectors.toList()));
    }

    public boolean availableAndAppliable() {
        return allAvailable() && allAppliable();
    }

    /**
     * Return a
     * 
     * @return First item
     */
    public Item get() {
        return firstAvailableOrNotFound();
    }

    /**
     * Get a suitable item matching all attributes
     * 
     * @param attributes
     * @return An item that matches all attributes, or the first available, or {@link Item#NotFound}.
     */

    public final Item get(Enum<?>... attributes) {
        return getInternal(attributes);
    }

    public final Item get(String... attributes) {
        return getInternal(attributes);
    }

    /**
     * Get first matching or any.
     * 
     * @return
     */
    @SafeVarargs
    private final <S> Item getInternal(S... attributes) {
        if (attributes.length == 0) {
            return firstAvailableOrNotFound();
        } else {
            for (Item item : this) {
                if (item.is(attributes)) {
                    return item;
                }
            }
        }
        return Item.NotFound;
    }

    private Item firstAvailableOrNotFound() {
        List<Item> available = getAvailable();
        if (!available.isEmpty()) {
            return available.get(0);
        } else if (!isEmpty()) {
            return get(0);
        } else {
            return Item.NotFound;
        }
    }

    public Items all() {
        return new Items(this);
    }

    public final Items getAll(Enum<?>... attributes) {
        return getAllImpl(attributes);
    }

    public final Items getAll(String... attributes) {
        return getAllImpl(attributes);
    }

    @SafeVarargs
    public final <S> Items getAllImpl(S... attributes) {
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

    public boolean contains(Enum<?> item) {
        return containsImpl(item);
    }

    public boolean contains(String item) {
        return containsImpl(item);
    }

    private <S> boolean containsImpl(S item) {
        for (Item i : this) {
            if (qualifiedItem(i).equals(item)) {
                return true;
            }
        }
        return false;
    }

    private static QualifiedItem qualifiedItem(Item item) {
        if (item instanceof ItemProxy) {
            return QualifiedItem.of(((ItemImpl) ((ItemProxy) item).item).item);
        } else if (item instanceof ItemImpl) {
            return QualifiedItem.of(((ItemImpl) item).item);
        } else
            throw new UnsupportedOperationException(item.toString());
    }

    /**
     * Get matching or available items. Try to match items with the requested attributes first. If not possible, match
     * as many items as possible, then complete set with available non-matching items. Finally add unavailable items to
     * complete the set.
     * 
     * @param attributes
     *            The preferred attributes to match.
     * @return Preferred items matching requested attributes, filled up with non-matching items as a fall-back.
     */
    @SafeVarargs
    public final <S> Items prefer(S... attributes) {
        Set<QualifiedItem> found = new HashSet<>();
        Items preferred = new Items();

        for (Item item : this) {
            if (item.is(attributes)) {
                found.add(QualifiedItem.of(item));
                preferred.add(item);
            }
        }

        for (Item item : this) {
            if (!found.contains(QualifiedItem.of(item)) && item.isAvailable()) {
                found.add(QualifiedItem.of(item));
                preferred.add(item);
            }
        }

        for (Item item : this) {
            if (!found.contains(QualifiedItem.of(item))) {
                found.add(QualifiedItem.of(item));
                preferred.add(item);
            }
        }

        return preferred;
    }

    /**
     * Select matching items and return a set of them. Build all combinations of available items, see if they could be
     * applied, choose one of the candidates.
     * 
     * @param prefrerred
     *            Preferred attributes of the selected set.
     * 
     * @return
     */
    public <S> Items selectAppliableSet(@SuppressWarnings("unchecked") S... prefrerred) {
        // TODO Implement this - at least just return one item of each kind
        return new Items(this);
    }
}
