package teaselib.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import teaselib.core.state.AbstractProxy;
import teaselib.core.util.QualifiedItem;
import teaselib.util.math.Combinations;
import teaselib.util.math.Varieties;

/**
 * Represents a set of items to be used.
 * <p>
 * Items can be queried in a couple of ways, allowing the script to request items with certain attributes, falling back
 * to available items, dealing with non-availability and asking for the "best" set.
 * <p>
 * Most methods that work with single items can be use with multiple items too.
 * <p>
 * Before performing queries ({@link Items#prefer}, {@link Items#matching},{@link Items#queryInventory}), the
 * non-index-based methods use or return always the item that is applied, or the first available. A a result, applying
 * multiple items works as if applying single items of each kind one-by-one.
 * <p>
 * However, wWhen performing a {@link Items#prefer} or {@link Items#query} command, only the requested item instances
 * are retained.
 * 
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

    public Items filter(Predicate<? super Item> predicate) {
        List<Item> list = stream().filter(predicate).collect(Collectors.toList());
        return new Items(list);
    }

    public boolean anyAvailable() {
        return stream().anyMatch(Item::isAvailable);
    }

    public boolean allAvailable() {
        return stream().allMatch(Item::isAvailable);
    }

    public boolean anyApplicable() {
        return stream().anyMatch(Item::canApply);
    }

    public boolean allApplicable() {
        return stream().allMatch(Item::canApply);
    }

    public boolean anyApplied() {
        return stream().anyMatch(Item::applied);
    }

    public boolean allApplied() {
        return stream().allMatch(Item::applied);
    }

    public boolean someAre(Object... attributes) {
        return stream().anyMatch(item -> item.is(attributes));
    }

    public boolean allAre(Object... attributes) {
        return stream().allMatch(item -> item.is(attributes));
    }

    public boolean anyExpired() {
        return stream().anyMatch(Item::expired);
    }

    public boolean allExpired() {
        return stream().allMatch(Item::expired);
    }

    public Items getAvailable() {
        return new Items(filter(Item::isAvailable));
    }

    public Items getApplicable() {
        return new Items(filter(Item::canApply));
    }

    public Items getApplied() {
        return new Items(filter(Item::applied));
    }

    /**
     * Return applied or first available item.
     * 
     * @return First item or {@link Item#NotFound}
     */
    public Item get() {
        return getAppliedOrFirstAvailableOrNotFound();
    }

    /**
     * Return applied or first available item with the supplied value.
     * 
     * @return First item or {@link Item#NotFound}
     */
    public final Item get(Enum<?> item) {
        return item(QualifiedItem.of(item));
    }

    /**
     * Return applied or first available item with the supplied value.
     * 
     * @return First item or {@link Item#NotFound}
     */
    public final Item item(String item) {
        return item(QualifiedItem.of(item));
    }

    private final Item item(QualifiedItem item) {
        for (Item mine : this.firstOfEachKind()) {
            if (QualifiedItem.of(mine).equals(item)) {
                return mine;
            }
        }
        return Item.NotFound;
    }

    private Item getAppliedOrFirstAvailableOrNotFound() {
        List<Item> applied = getApplied();
        if (!applied.isEmpty()) {
            return applied.get(0);
        } else {
            List<Item> available = getAvailable();
            if (!available.isEmpty()) {
                return available.get(0);
            } else if (!isEmpty()) {
                return get(0);
            } else {
                return Item.NotFound;
            }
        }
    }

    public List<Item> all() {
        return new ArrayList<>(this);
    }

    /**
     * Get all items matching the supplied attributes. Only available items are returned.
     * 
     * @param attributes
     * @return Available items that match all of the attributes.
     */
    public final Items matching(Enum<?>... attributes) {
        return matchingImpl((Object[]) attributes);
    }

    public final Items matching(String... attributes) {
        return matchingImpl((Object[]) attributes);
    }

    public final Items matchingImpl(Object... attributes) {
        Items matching = queryInventoryImpl(attributes);
        return matching.getAvailable();
    }

    /**
     * Get all items matching the supplied attributes. This method also returns non-available items.
     * 
     * @param attributes
     * @return Available items that match all of the attributes.
     */
    /**
     * @param attributes
     * @return
     */
    public final Items queryInventory(Enum<?>... attributes) {
        return queryInventoryImpl((Object[]) attributes);
    }

    public final Items queryInventory(String... attributes) {
        return queryInventoryImpl((Object[]) attributes);
    }

    private Items queryInventoryImpl(Object... attributes) {
        Items matching;
        if (attributes.length == 0) {
            matching = new Items(this);
        } else {
            matching = new Items();
            for (Item item : this) {
                if (itemImpl(item).has(attributes)) {
                    matching.add(item);
                }
            }
        }
        return matching;
    }

    public boolean contains(Enum<?> item) {
        return containsImpl(item);
    }

    public boolean contains(String item) {
        return containsImpl(item);
    }

    private <S> boolean containsImpl(S item) {
        for (Item i : this) {
            if (QualifiedItem.of(i).equals(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get matching or available items:
     * <li>First try to match available items with the requested attributes.
     * <li>If not possible, match as many items as possible. then complete set with available non-matching items.
     * <li>Finally add unavailable items to complete the set.
     * <p>
     * 
     * @param attributes
     *            The preferred attributes to match.
     * @return Preferred available items matching requested attributes, filled up with non-matching available items as a
     *         fall-back. The Item list may be empty if none of the requesteed items are available.
     */
    public <S> Items prefer(Enum<?>... attributes) {
        return preferImpl((Object[]) attributes);
    }

    public <S> Items prefer(String... attributes) {
        return preferImpl((Object[]) attributes);
    }

    private Items preferImpl(Object... attributes) {
        Set<QualifiedItem> found = new HashSet<>();
        Items preferred = new Items();

        for (Item item : this) {
            if (item.is(attributes) && item.isAvailable()) {
                found.add(QualifiedItem.of(itemValue(item)));
                preferred.add(item);
            }
        }

        for (Item item : this) {
            if (!found.contains(QualifiedItem.of(item)) && item.isAvailable()) {
                found.add(QualifiedItem.of(itemValue(item)));
                preferred.add(item);
            }
        }

        return preferred;
    }

    public Items reduce(BinaryOperator<Items> accumulator) {
        return varieties().reduce(accumulator);
    }

    /**
     * Return all combinations of item sets:
     * <li>All combinations of the items are returned, the resulting sets will contain one item per kind. To receive
     * combinations that match the intention of the script, use:
     * <li>{@link Items#query} to retain only the items that match a specific criteria
     * <li>{@link Items#prefer} to retain the items that match a specific criteria, or any other available item. Get the
     * best combination of items via {@link Varieties#reduce} with argument {@link Items#best}
     * 
     * @return
     */
    public Varieties<Items> varieties() {
        int variety = getVariety();
        Combinations<Item[]> combinations = Combinations.combinationsK(variety, toArray());
        return combinations.stream().map(Arrays::asList).filter(this::isVariety).map(Items::new)
                .collect(Varieties.toVarieties());
    }

    private boolean isVariety(List<Item> combination) {
        return Varieties.isVariety(
                combination.stream().map(QualifiedItem::of).map(QualifiedItem::toString).collect(Collectors.toList()));
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
     * Select the items that match best. This can be the one that has the most attributes in common, or something the
     * user has pre-selected via the user interface (like the "Dresser App" that has been around a few years ago).
     * 
     * @param itemsA
     * @param itemsB
     * @return The items that is match better.
     */
    public static Items best(Items itemsA, Items itemsB) {
        // Count unique attributes found, then for each attribute add numberOfOccurences*count to rate sets higher
        long maxA = attributesOfAvailable(itemsA).values().stream().reduce(Math::max).orElse(0L);
        long maxB = attributesOfAvailable(itemsB).values().stream().reduce(Math::max).orElse(0L);
        return maxA >= maxB ? itemsA : itemsB;
    }

    private static Map<QualifiedItem, Long> attributesOfAvailable(Items items) {
        return items.stream().filter(Item::isAvailable)
                .collect(Collectors.groupingBy(QualifiedItem::of, Collectors.counting()));
    }

    /**
     * Select matching items and return a set of them. Build all combinations of available items, see if they can be
     * applied, choose one of the candidates.
     * 
     * @return
     */
    public Items best() {
        return varieties().reduce(Items::best);
    }

    /**
     * Applies each item. If the list contains multiple items of the same kind, only the first of each kind is applied.
     * 
     * @return the items.
     */
    public Items apply() {
        for (Item item : this.firstOfEachKind()) {
            item.apply();
        }
        return this;
    }

    /**
     * Applies each item to the given peers. If there are multiple items of the same kind, only the first of each kind
     * is applied. To apply to multiple instances, apply each through {@link Items#all}
     * 
     * @return the items.
     */
    public Items applyTo(Object... peers) {
        for (Item item : this.firstOfEachKind()) {
            item.applyTo(AbstractProxy.removeProxies(peers));
        }
        return this;
    }

    public Items over(long duration, TimeUnit unit) {
        for (Item item : this.firstOfEachKind()) {
            item.apply().over(duration, unit);
        }
        return this;
    }

    public void remove() {
        for (Item item : this.firstOfEachKind()) {
            item.remove();
        }
    }

    public Items firstOfEachKind() {
        Items firstOfEachKind = new Items();
        Set<QualifiedItem> kinds = new HashSet<>();

        for (Object item : this.valueSet()) {
            QualifiedItem kind = QualifiedItem.of(item);
            if (!kinds.contains(kind)) {
                kinds.add(kind);
                firstOfEachKind.add(getFirstAvailableOrNotFound(item));
            }
        }
        return firstOfEachKind;
    }

    private Item getFirstAvailableOrNotFound(Object item) {
        Item firstAvailable = getApplied().findFirst(item);
        if (firstAvailable == Item.NotFound) {
            firstAvailable = getAvailable().findFirst(item);
            if (firstAvailable == Item.NotFound) {
                firstAvailable = findFirst(item);
            }
        }
        return firstAvailable;
    }

    private Item findFirst(Object item) {
        Optional<Item> first = stream().filter(i -> i.is(item)).findFirst();
        if (first.isPresent()) {
            return first.get();
        } else {
            return Item.NotFound;
        }
    }

    /**
     * Get a sublist just containing the requested items
     * 
     * @return A sublist containing the requested items, or {@link Item#NotFound} for any missing item.
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

    public Set<Object> valueSet() {
        return stream().map(item -> itemImpl(item).value).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static ItemImpl itemImpl(Item item) {
        return AbstractProxy.itemImpl(item);
    }

    private static QualifiedItem itemValue(Item item) {
        return QualifiedItem.of(itemImpl(item).value);
    }
}
