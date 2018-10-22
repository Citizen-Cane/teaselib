package teaselib.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import teaselib.core.state.ItemProxy;
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

    public Items filter(Predicate<? super Item> predicate) {
        List<Item> list = stream().filter(predicate).collect(Collectors.toList());
        return new Items(list);
    }

    public boolean anyAvailable() {
        return stream().filter(Item::isAvailable).count() > 0;
    }

    public boolean allAvailable() {
        return stream().filter(Item::isAvailable).count() == size();
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

    public boolean anyExpired() {
        return stream().filter(Item::expired).count() > 0;
    }

    public boolean allExpired() {
        return stream().filter(Item::expired).count() == size();
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
        return getAppliedOrFirstAvailableOrNotFound();
    }

    public final Item get(Enum<?> item) {
        return item(QualifiedItem.of(item));
    }

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

    public Items all() {
        return new Items(this);
    }

    /**
     * Get all items matching the supplied attributes
     * 
     * @param attributes
     * @return An item that matches all attributes, or the first available, or {@link Item#NotFound}.
     */
    @SafeVarargs
    public final Items query(Enum<?>... attributes) {
        return queryImpl(attributes);
    }

    public final Items query(String... attributes) {
        return queryImpl(attributes);
    }

    @SafeVarargs
    public final <S> Items queryImpl(S... attributes) {
        if (attributes.length == 0) {
            return this;
        } else {
            Items matching = new Items();
            for (Item item : this) {
                if (itemImpl(item).has(attributes)) {
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
     * Select matching items and return a set of them. Build all combinations of available items, see if they can be
     * applied, choose one of the candidates.
     * 
     * @param prefrerred
     *            Preferred attributes of the selected set.
     * 
     * @return
     */
    public <S> Items selectApplicableSet(@SuppressWarnings("unchecked") S... preferred) {
        // TODO improve this - at least just return one item of each kind
        return prefer(preferred).getAvailable();
    }

    public Items apply() {
        for (Item item : this.firstOfEachKind()) {
            item.apply();
        }
        return this;
    }

    // TODO Split to string and enum, since states would be passed in as proxies
    public Items applyTo(Object... peers) {
        for (Item item : this.firstOfEachKind()) {
            item.applyTo(removeProxies(peers));
        }
        return this;
    }

    private static Object[] removeProxies(Object... peers) {
        Object[] proxiesRemoved = new Object[peers.length];
        for (int i = 0; i < peers.length; i++) {
            proxiesRemoved[i] = removeProxy(peers[i]);
        }
        return proxiesRemoved;
    }

    private static Object removeProxy(Object item) {
        if (item instanceof Item) {
            return itemImpl((Item) item);
        } else {
            return item;
        }
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

    private Items firstOfEachKind() {
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

    public Object[] valueSet() {
        return stream().map(item -> itemImpl(item).item).collect(Collectors.toCollection(LinkedHashSet::new))
                .toArray(new Object[0]);
    }

    private static ItemImpl itemImpl(Item item) {
        if (item instanceof ItemProxy)
            return itemImpl(((ItemProxy) item).item);
        else
            return (ItemImpl) item;
    }
}
