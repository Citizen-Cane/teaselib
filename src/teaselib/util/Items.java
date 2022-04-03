package teaselib.util;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toCollection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import teaselib.Duration;
import teaselib.State;
import teaselib.State.Persistence;
import teaselib.TeaseScriptPersistence;
import teaselib.core.state.AbstractProxy;
import teaselib.core.util.QualifiedString;
import teaselib.util.math.Combinations;
import teaselib.util.math.Random;
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
 * However, wWhen performing a {@link Items#prefer} or {@link Items#attire} command, only the requested item instances
 * are retained.
 * <p>
 * TODO use sets the user has pre-selected via the user interface (like the "Dresser App" that has been around a few
 * years ago).
 * 
 * @author Citizen-Cane
 *
 */
public class Items implements Iterable<Item> {

    public static final Items None = new Items(Collections.emptyList());

    // TODO rename get() to available
    // TODO implement oneOfEach(), avail or not, similar to inventory but only on of each kind
    // TODO review all calls to item() and replace with item(random)
    public interface Query {

        public static final Query None = ItemsQueryImpl.None;

        Query prefer(Enum<?>... values);

        Query prefer(String... values);

        Query matching(Enum<?>... values);

        Query matching(String... values);

        Query matchingAny(Enum<?>... values);

        Query matchingAny(String... values);

        Query without(Enum<?>... values);

        Query without(String... values);

        Query orElseItems(Enum<?>... items);

        Query orElseItems(String... items);

        Query orElsePrefer(Enum<?>... attributes);

        Query orElsePrefer(String... attributes);

        Query orElseMatching(Enum<?>... attributes);

        Query orElseMatching(String... attributes);

        Query orElse(Items.Query items);

        boolean noneApplied();

        boolean noneAvailable();

        boolean noneApplicable();

        boolean anyApplied();

        boolean anyAvailable();

        boolean anyApplicable();

        /**
         * Determine whether the query result would contain at least one applied item of each element of its value set.
         * <p>
         * This does not necessarily mean that all items in the query are applied, but the value set of
         * {@link Items.Query#getApplied} would be equal to this item query' {@link Items#valueSet}.
         * 
         * @return Whether all items are applied.
         */
        boolean allApplied();

        /**
         * Determine whether the query result would contain at least one available item of each element of its value
         * set.
         * <p>
         * This does not necessarily mean that all items in the query are available, but the value set of
         * {@link Items.Query#getAvailable} would be equal to this item query' {@link Items#valueSet}.
         * 
         * @return Whether all items are applied.
         */
        boolean allAvailable();

        /**
         * Determine whether the query result would contain at least one applicable item of each element of its value
         * set.
         * <p>
         * This does not necessarily mean that all items in the query are available, but the value set of
         * {@link Items.Query#getApplicable} would be equal to this item query' {@link Items#valueSet}.
         * 
         * @return Whether all items are applied.
         */
        boolean allApplicable();

        /**
         * Return the applied item, a random available or an unavailable item.
         * 
         * @return A valid item, or Item.NotFound if the query did not select any item.
         */
        Item item();

        /**
         * Select a set of items, one per kind, taking in account already applied items.
         * <p>
         * Only applicable items are considered. This means that the returned set does not contain applied or
         * unavailable items.
         * 
         * @return A set of items that can be applied, one per available kind.
         */
        Items getApplicableSet();

        /**
         * Get all items that are currently applied.
         * 
         * @return All applied items.
         */
        Items getApplied();

        /**
         * Get all items that are currently available.
         * 
         * @return All available items.
         */
        Items getAvailable();

        /**
         * Get all items that are currently applied.
         * 
         * @return All applied items.
         */
        Items getApplicable();

        /**
         * Get all items that are defined in the inventory. This also includes items that are defined but not available.
         * 
         * @return All defined items in the inventory.
         */
        Items inventory();
    }

    final Random random;
    private final List<Item> elements;
    private final List<Item> inventory;

    public Items(Items items) {
        this(new ArrayList<>(items.elements), items.inventory);
    }

    public Items(Item... items) {
        this(Arrays.asList(items));
    }

    public Items(Items... items) {
        this(Arrays.stream(items).flatMap(Items::stream).toList());
    }

    public Items(Set<Item> items) {
        this(Collections.unmodifiableList(new ArrayList<>(items)),
                Collections.unmodifiableList(new ArrayList<>(items)));
    }

    public Items(List<Item> items) {
        this(Collections.unmodifiableList(new ArrayList<>(items)),
                Collections.unmodifiableList(new ArrayList<>(items)));
    }

    private Items(List<Item> elements, List<Item> inventory) {
        this.random = random(elements, inventory);
        this.elements = elements;
        this.inventory = inventory;
    }

    private static Random random(List<Item> elements, List<Item> inventory) {
        return Stream.concat(elements.stream(), inventory.stream()).filter(Predicate.not(Item.NotFound::equals))
                .map(AbstractProxy::removeProxy).filter(ItemImpl.class::isInstance).map(ItemImpl.class::cast)
                .map(item -> item.teaseLib.random).findFirst().orElse(null);
    }

    public Items filter(Predicate<? super Item> predicate) {
        List<Item> list = elements.stream().filter(predicate).toList();
        return new Items(list, inventory);
    }

    public boolean noneAvailable() {
        return elements.stream().noneMatch(Item::isAvailable);
    }

    public boolean anyAvailable() {
        return elements.stream().anyMatch(Item::isAvailable);
    }

    public boolean allAvailable() {
        if (elements.isEmpty()) {
            return false;
        }
        return elements.stream().allMatch(Item::isAvailable);
    }

    public boolean noneApplicable() {
        return elements.stream().noneMatch(Item::canApply);
    }

    public boolean anyApplicable() {
        return elements.stream().anyMatch(Item::canApply);
    }

    public boolean allApplicable() {
        if (elements.isEmpty()) {
            return false;
        }
        return elements.stream().allMatch(Item::canApply);
    }

    public boolean noneApplied() {
        return elements.stream().noneMatch(Item::applied);
    }

    public boolean anyApplied() {
        return elements.stream().anyMatch(Item::applied);
    }

    public boolean allApplied() {
        if (elements.isEmpty()) {
            return false;
        }
        return elements.stream().allMatch(Item::applied);
    }

    public boolean noneRemoved() {
        return elements.stream().noneMatch(Item::removed);
    }

    public boolean anyRemoved() {
        return elements.stream().anyMatch(Item::removed);
    }

    public boolean allRemoved() {
        if (elements.isEmpty()) {
            return false;
        }
        return elements.stream().allMatch(Item::removed);
    }

    /**
     * Returns the time span since the last of the items has been removed.
     * 
     * @param unit
     * @return The time span since the last of the item have been removed, or 0 if any is still applied.
     */
    public long removed(TimeUnit unit) {
        return elements.stream().map(item -> item.removed(unit)).reduce(Math::min).orElse(666L);
    }

    public boolean anyAre(Object... attributes) {
        return elements.stream().anyMatch(item -> item.is(attributes));
    }

    public boolean allAre(Object... attributes) {
        if (elements.isEmpty()) {
            return false;
        }
        return elements.stream().allMatch(item -> item.is(attributes));
    }

    public boolean anyExpired() {
        if (elements.isEmpty()) {
            return true;
        }
        return elements.stream().anyMatch(Item::expired);
    }

    public boolean allExpired() {
        return elements.stream().allMatch(Item::expired);
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

    public Items getFree() {
        return new Items(filter(Predicate.not(Item::applied)));
    }

    public Items getExpired() {
        return new Items(filter(Item::expired));
    }

    /**
     * Return applied or random available item.
     * 
     * @return First item or {@link Item#NotFound}
     */
    // TODO when get(Enum<?>) is removed because of item(), rename this to first()
    public Item get() {
        return getAppliedOrAvailableOrNotFound(random);
    }

    /**
     * Return applied or first available item with the supplied value.
     * 
     * @return First item or {@link Item#NotFound}
     */
    // TODO get() and item() are the same ->
    // + keep get() since item(...).item(...) is confusing - I misinterpreted it myself
    public final Item get(Enum<?> item) {
        return item(item);
    }

    public final Item get(int index) {
        return elements.get(index);
    }

    /**
     * Return applied or first available item with the supplied value.
     * 
     * @return First item or {@link Item#NotFound}
     */
    public final Item item(Enum<?> item) {
        return item(QualifiedString.of(item));
    }

    public final Item item(String item) {
        return item(QualifiedString.of(item));
    }

    private final Item item(QualifiedString item) {
        if (item.guid().isPresent()) {
            throw new UnsupportedOperationException("Support named items");
        }

        return oneOfEachKind().stream().filter(element -> {
            return item.equals(AbstractProxy.removeProxy(element).kind());
        }).findFirst().orElse(Item.NotFound);
    }

    private Item getAppliedOrAvailableOrNotFound(Random random) {
        List<Item> applied = getApplied().elements;
        if (!applied.isEmpty()) {
            return applied.get(0);
        } else {
            return getAvailableOrNotFound(random);
        }
    }

    private Item getAvailableOrNotFound(Random random) {
        List<Item> available = getAvailable().elements;
        if (!available.isEmpty()) {
            if (random != null) {
                return random.item(available);
            } else {
                return available.get(0);
            }
        } else if (!elements.isEmpty()) {
            if (random != null) {
                return random.item(elements);
            } else {
                return elements.get(0);
            }
        } else {
            return Item.NotFound;
        }
    }

    /**
     * Get all items matching the supplied attributes. All items are returned, as a result the collection may contains
     * unavailable items.
     * 
     * @param attributes
     * @return Items that match all of the attributes.
     */
    public final Items matching(Enum<?>... attributes) {
        return matching(Arrays.asList(attributes));
    }

    public final Items matching(String... attributes) {
        return matching(Arrays.asList(attributes));
    }

    private Items matching(List<? extends Object> attributes) {
        Items matchingItems;
        if (attributes.isEmpty()) {
            matchingItems = new Items(this);
        } else {
            List<Item> matching = elements.stream().filter(item -> item.is(attributes)).toList();
            matchingItems = new Items(matching, inventory);
        }
        return matchingItems;
    }

    public final Items matchingAny(Enum<?>... attributes) {
        return matchingAny(Arrays.asList(attributes));
    }

    public final Items matchingAny(String... attributes) {
        return matchingAny(Arrays.asList(attributes));
    }

    private Items matchingAny(List<? extends Object> attributes) {
        Items matchingItems;
        if (attributes.isEmpty()) {
            matchingItems = new Items(this);
        } else {
            List<Item> matching = elements.stream().filter(item -> isAny(item, attributes)).toList();
            matchingItems = new Items(matching, inventory);
        }
        return matchingItems;
    }

    boolean isAny(Item item, List<? extends Object> attributes) {
        for (Object object : attributes) {
            if (item.is(object)) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(Enum<?> item) {
        return containsImpl(QualifiedString.of(item));
    }

    public boolean contains(String item) {
        return containsImpl(QualifiedString.of(item));
    }

    public boolean contains(Item item) {
        return elements.contains(item);
    }

    public boolean containsAny(Items items) {
        return !intersection(items).isEmpty();
    }

    public boolean containsAll(Items items) {
        return elements.containsAll(items.elements);
    }

    public Items intersection(Items items) {
        return new Items(elements.stream().distinct().filter(items::contains).toList());
    }

    private boolean containsImpl(QualifiedString kind) {
        return elements.stream().map(AbstractProxy::removeProxy).map(ItemImpl::kind).anyMatch(kind::is);
    }

    /**
     * Get applied, matching or available items:
     * <li>First prefer applied items.
     * <li>second try to match available items with the requested attributes.
     * <li>If not possible, match as many items as possible. then complete set with available non-matching items.
     * <li>Continue adding unavailable items that match the requested attributes.
     * <li>Finally add unavailable items that match the requested item to complete the set.
     * <p>
     * 
     * @param attributes
     *            The preferred attributes to match.
     * @return Preferred applied and available items matching requested attributes, filled up with non-matching
     *         available items as a fall-back. The result may be empty if none of the requested items are available.
     */
    public Items prefer(Enum<?>... attributes) {
        return preferredItems((Object[]) attributes);
    }

    public Items prefer(String... attributes) {
        return preferredItems((Object[]) attributes);
    }

    static class Preferred {
        private final List<Item> elements;
        private final Object[] attributes;
        private final Set<QualifiedString> found = new HashSet<>();
        private final List<Item> items = new ArrayList<>();

        public Preferred(List<Item> elements, Object[] attributes) {
            this.elements = elements;
            this.attributes = attributes;
        }

        // TODO Optimize items by number of matching attributes - best matching set with attribute coverage
        // e.g. all lockable, or all metal, use attribute order as priority

        public void addAvailableMatching() {
            for (Item item : elements) {
                // Add all matching, select set later for variance
                if (item.is(attributes) && item.isAvailable()) {
                    found.add(itemValue(item));
                    items.add(item);
                }
            }
        }

        public void addAvailableNonMatching() {
            for (Item item : elements) {
                if (missing(item) && item.isAvailable()) {
                    found.add(itemValue(item));
                    items.add(item);
                }
            }
        }

        public void addMissingMatching() {
            for (Item item : elements) {
                if (missing(item) && item.is(attributes)) {
                    found.add(itemValue(item));
                    items.add(item);
                }
            }
        }

        public void addMissing() {
            for (Item item : elements) {
                if (missing(item)) {
                    found.add(itemValue(item));
                    items.add(item);
                }
            }
        }

        private boolean missing(Item item) {
            return !found.contains(itemValue(item));
        }

        public List<Item> toList() {
            return items;
        }

    }

    private Items preferredItems(Object... attributes) {
        Preferred preferred = new Preferred(elements, attributes);
        preferred.addAvailableMatching();
        preferred.addAvailableNonMatching();
        preferred.addMissingMatching();
        preferred.addMissing();
        return new Items(preferred.toList(), inventory);
    }

    /**
     * Return all combinations of item sets:
     * <li>All combinations of the items are returned, the resulting sets will contain one item per kind. To receive
     * combinations that match the intention of the script, use:
     * <li>{@link Items#attire} to retain only the items that match a specific criteria
     * <li>{@link Items#prefer} to retain the items that match a specific criteria, or any other available item. Get the
     * best combination of items via {@link Varieties#reduce} with argument {@link Items#best}
     * 
     * @return
     */
    Varieties<Items> varieties() {
        int variety = getVariety();
        Combinations<Item[]> combinations = Combinations.combinationsK(variety, toArray());
        return combinations.stream().map(Arrays::asList).filter(this::isVariety).map(Items::new)
                .collect(Varieties.toVarieties());
    }

    private boolean isVariety(List<Item> combination) {
        return Varieties.isVariety(combination.stream().map(AbstractProxy::itemImpl).map(itemImpl -> itemImpl.kind())
                .map(QualifiedString::toString).toList());
    }

    private int getVariety() {
        Set<String> types = new HashSet<>();
        for (Item item : elements) {
            types.add(AbstractProxy.itemImpl(item).kind().toString());
        }

        return types.size();
    }

    public Item[] toArray() {
        Item[] array = new Item[elements.size()];
        return elements.toArray(array);
    }

    /**
     * Select the items that match best:
     * <li>prefer sets that have items already applied
     * <li>Choose the set that has the most attributes in common
     * 
     * @param itemsA
     * @param itemsB
     * @return The items that is match better.
     */
    static Items best(Items itemsA, Items itemsB) {
        // TODO Improve attribute matching for applied items
        // - decide whether to consider preferred or matching attributes
        // - currently there is no attribute matching at all
        long a = itemsA.getAvailable().elements.size();
        long b = itemsB.getAvailable().elements.size();
        if (a == b) {
            // Count unique attributes found, then for each attribute add numberOfOccurences*count to rate sets higher
            a = attributesOfAvailable(itemsA.elements).values().stream().reduce(Math::max).orElse(0L);
            b = attributesOfAvailable(itemsB.elements).values().stream().reduce(Math::max).orElse(0L);
        }
        return a >= b ? itemsA : itemsB;
    }

    private static Map<QualifiedString, Long> attributesOfAvailable(List<Item> items) {
        return items.stream().filter(Item::isAvailable).collect(groupingBy(QualifiedString::of, counting()));
    }

    /**
     * Applies each item. If the list contains multiple items of the same kind, only the first of each kind is applied.
     * 
     */
    public State.Options apply() {
        return applyToImpl(Item::apply);
    }

    /**
     * Applies each item to the given peers. If there are multiple items of the same kind, only the first of each kind
     * is applied. To apply to multiple instances, iterate over the elements and apply each with {@link Item#apply}
     * 
     */
    public State.Options applyTo(Object... peers) {
        return applyToImpl(item -> item.applyTo(peers));
    }

    private State.Options applyToImpl(Function<Item, State.Options> applyFunction) {
        List<State.Options> options = oneOfEachKind().stream().map(applyFunction::apply).toList();
        return new State.Options() {
            @Override
            public void remember(Until forget) {
                options.forEach(item -> item.remember(forget));
            }

            @Override
            public Persistence over(long duration, TimeUnit unit) {
                options.forEach(option -> option.over(duration, unit));
                return this;
            }

            @Override
            public Persistence over(Duration duration) {
                options.forEach(option -> option.over(duration));
                return this;
            }
        };
    }

    public void remove() {
        for (Item item : oneOfEachKind()) {
            item.remove();
        }
    }

    public void removeFrom(Enum<?>... peers) {
        for (var item : oneOfEachKind()) {
            item.removeFrom((Object[]) peers);
        }
    }

    public void removeFrom(String... peers) {
        for (var item : oneOfEachKind()) {
            item.removeFrom((Object[]) peers);
        }
    }

    public Collection<Item> oneOfEachKind() {
        List<Item> firstOfEachKind = new ArrayList<>();
        Set<QualifiedString> kinds = new HashSet<>();

        for (QualifiedString item : this.valueSet()) {
            var kind = QualifiedString.of(item);
            if (!kinds.contains(kind)) {
                kinds.add(kind);
                firstOfEachKind.add(getAppliedOrRandomAvailableOrNotFound(item));
            }
        }
        return firstOfEachKind;
    }

    private Item getAppliedOrRandomAvailableOrNotFound(QualifiedString item) {
        Item available = getApplied().findFirst(item);
        if (available == Item.NotFound) {
            available = getAvailable().random(item);
            if (available == Item.NotFound) {
                available = random(item);
            }
        }
        return available;
    }

    private Item findFirst(QualifiedString item) {
        Optional<Item> first = elements.stream().filter(i -> i.is(item)).findFirst();
        if (first.isPresent()) {
            return first.get();
        } else {
            return Item.NotFound;
        }
    }

    private Item random(QualifiedString item) {
        var items = elements.stream().filter(i -> i.is(item)).toList();
        if (items.isEmpty()) {
            return Item.NotFound;
        } else {
            return random.item(items);
        }
    }

    /**
     * Get a sublist just containing the requested items
     * 
     * @return A sublist containing the requested items, or {@link Item#NotFound} for any missing item.
     */
    public Items items(Enum<?>... anyItemOrAttribute) {
        return itemsImpl((Object[]) anyItemOrAttribute);
    }

    public Items items(String... anyItemOrAttribute) {
        return itemsImpl((Object[]) anyItemOrAttribute);
    }

    public Items items(Select.Statement query) {
        return query.get(this);
    }

    private Items itemsImpl(Object... anyItemOrAttribute) {
        List<Item> items = new ArrayList<>();
        for (Item item : elements) {
            for (Object any : anyItemOrAttribute) {
                if (item.is(any)) {
                    items.add(item);
                }
            }
        }
        return new Items(items, inventory);
    }

    @Override
    public Iterator<Item> iterator() {
        return elements.iterator();
    }

    public int size() {
        return elements.size();
    }

    public Stream<Item> stream() {
        return elements.stream();
    }

    /**
     * return distinct item values.
     * 
     * @return
     */
    Set<QualifiedString> valueSet() {
        return elements.stream().map(Items::itemImpl).map(ItemImpl::kind).collect(toCollection(LinkedHashSet::new));
    }

    private static ItemImpl itemImpl(Item item) {
        return AbstractProxy.itemImpl(item);
    }

    static QualifiedString itemValue(Item item) {
        return itemImpl(item).kind();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((elements == null) ? 0 : elements.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Items other = (Items) obj;
        if (elements == null) {
            if (other.elements != null)
                return false;
        } else if (!elements.equals(other.elements))
            return false;
        return true;
    }

    public List<Item> addTo(List<Item> list) {
        list.addAll(elements);
        return list;
    }

    public static List<Item> addTo(List<Item> list, Items items) {
        return items.addTo(list);
    }

    public Items orElseItems(Enum<?>... items) {
        return anyAvailable() ? this : new Items(inventory).items(items);
    }

    public Items orElseItems(String... items) {
        return anyAvailable() ? this : new Items(inventory).items(items);
    }

    public Items orElsePrefer(Enum<?>... attributes) {
        return anyAvailable() ? this : new Items(inventory).prefer(attributes);
    }

    public Items orElsePrefer(String... attributes) {
        return anyAvailable() ? this : new Items(inventory).prefer(attributes);
    }

    public Items orElseMatching(Enum<?>... attributes) {
        return anyAvailable() ? this : new Items(inventory).matching(attributes);
    }

    public Items orElseMatching(String... attributes) {
        return anyAvailable() ? this : new Items(inventory).matching(attributes);
    }

    public Items orElse(Supplier<Items> items) {
        return anyAvailable() ? this : items.get();
    }

    public Items without(Enum<?>... values) {
        return withoutImpl((Object[]) values);
    }

    public Items without(String... values) {
        return withoutImpl((Object[]) values);

    }

    private Items withoutImpl(Object... values) {
        return new Items(stream().filter(item -> Arrays.stream(values).noneMatch(item::is)).toList(), inventory);
    }

    public Items of(TeaseScriptPersistence.Domain domain) {
        return domain.related(this);
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public String toString() {
        return "elements=" + displayNames(elements) + " inventory=" + displayNames(inventory);
    }

    private static String displayNames(List<Item> list) {
        return list.stream().map(Item::displayName).collect(Collectors.joining(" "));
    }

}
