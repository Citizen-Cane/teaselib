package teaselib.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import teaselib.core.util.QualifiedItem;
import teaselib.util.math.Combinations;
import teaselib.util.math.Varieties;

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

    public Items(Item[] items) {
        super(Arrays.asList(items));
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

    public boolean anyApplicable() {
        return stream().filter(Item::canApply).count() > 0;
    }

    public boolean allApplicable() {
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

    public Items getApplicable() {
        return new Items(filter(Item::canApply).collect(Collectors.toList()));
    }

    public Items getApplied() {
        return new Items(filter(Item::applied).collect(Collectors.toList()));
    }

    // TODO Changes meaning of canApply to allAvailable() && allApplicable() - but canApply is used to solely in
    // mine-maid to check body state
    @Deprecated
    public boolean usable() {
        return allAvailable() && allApplicable();
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

    public final Item get(Enum<?> item) {
        return item(QualifiedItem.of(item));
    }

    public final Item item(String item) {
        return item(QualifiedItem.of(item));
    }

    private final Item item(QualifiedItem item) {
        for (Item mine : this) {
            if (QualifiedItem.of(mine).equals(item)) {
                return mine;
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

    @SafeVarargs
    public final Items query(Enum<?>... attributes) {
        return getQueryImpl(attributes);
    }

    public final Items query(String... attributes) {
        return getQueryImpl(attributes);
    }

    @SafeVarargs
    public final <S> Items getQueryImpl(S... attributes) {
        if (attributes.length == 0) {
            return this;
        } else {
            Items matching = new Items();
            for (Item item : this) {
                if (item.is(attributes)) {
                    matching.add(item);
                }
            }
            return matching;
        }
    }

    public boolean contains(Enum<?> item) {
        return containsImpl(item);
    }

    public boolean contains(String item) {
        return containsImpl(item);
    }

    // TODO fails, similar to item(...) but without QualifiedItem wrap
    private <S> boolean containsImpl(S item) {
        for (Item i : this) {
            if (QualifiedItem.of(i).equals(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get matching or available items. Try to match available items with the requested attributes first. If not
     * possible, match as many items as possible, then complete set with available non-matching items. Finally add
     * unavailable items to complete the set.
     * 
     * @param attributes
     *            The preferred attributes to match.
     * @return Preferred items matching requested attributes, filled up with non-matching items as a fall-back.
     */
    @SafeVarargs
    // TODO Improve behavior and add more tests
    // TODO rate attributes in the order listed (first higher rated)
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

    public Varieties<Items> varieties() {
        int variety = getVariety();
        Combinations<Item[]> combinations = Combinations.combinationsK(variety, toArray());
        return combinations.stream().map(Arrays::asList)
                .filter(combination -> Varieties.isVariety(combination.stream().map(QualifiedItem::of)
                        .map(QualifiedItem::toString).collect(Collectors.toList())))
                .map(Items::new).collect(Varieties.toVarieties());
    }

    private int getVariety() {
        Set<String> types = new HashSet<>();
        for (Item item : this) {
            types.add(QualifiedItem.of(item).toString());
        }

        return types.size();
    }

    @Override
    public Item[] toArray() {
        Item[] array = new Item[size()];
        return super.toArray(array);
    }

    /**
     * Select the items that match best
     * 
     * @param a
     * @param b
     * @return
     */
    public static Items best(Items a, @SuppressWarnings("unused") Items b) {
        // TODO Define "best" and find the best item
        return a;
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
    public <S> Items selectApplicableSet(@SuppressWarnings("unchecked") S... preferred) {
        // TODO improve this - at least just return one item of each kind
        return prefer(preferred);
    }

    public void apply() {
        for (Item item : this) {
            item.apply();
        }
    }

    public void applyTo(Object... peers) {
        for (Item item : this) {
            item.applyTo(peers);
        }
    }

    public void remove() {
        for (Item item : this) {
            item.remove();
        }
    }

    /**
     * Get a list just containing the requested subset of items
     * 
     * @return A list containing the requested items, or {@link Item#NotFound} for any missing item.
     */
    public Items items(Enum<?>... itemOrAttribute) {
        Items items = new Items();
        for (Item item : this) {
            for (Enum<?> any : itemOrAttribute) {
                if (item.is(any)) {
                    items.add(item);
                }
            }
        }        
        return items;
    }
}
