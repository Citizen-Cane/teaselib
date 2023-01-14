package teaselib.core;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toCollection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import teaselib.Duration;
import teaselib.State;
import teaselib.State.Persistence;
import teaselib.core.state.AbstractProxy;
import teaselib.core.state.ItemProxy;
import teaselib.core.util.QualifiedString;
import teaselib.util.Item;
import teaselib.util.Items;
import teaselib.util.Select;
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
 * Before performing queries ({@link Items#prefer}, {@link Items#matcher},{@link Items#queryInventory}), the
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
public class ItemsImpl implements Items.Collection, Items.Set {

    // TODO instance with logger, for orElse(...)
    public static final ItemsImpl None = new ItemsImpl(Collections.emptyList());

    final List<Item> elements;
    final List<Item> inventory;
    final Random random;
    private final ItemLogger logger;

    private ItemsImpl(ItemsImpl items) {
        this(new ArrayList<>(items.elements), items.inventory);
    }

    public ItemsImpl(Item... items) {
        this(Arrays.asList(items));
    }

    public ItemsImpl(Items... items) {
        this(Arrays.stream(items).flatMap(Items::stream).toList());
    }

    public ItemsImpl(java.util.Collection<Item> items) {
        this(Collections.unmodifiableList(new ArrayList<>(items)),
                Collections.unmodifiableList(new ArrayList<>(items)));
    }

    public ItemsImpl(List<Item> items) {
        this(Collections.unmodifiableList(new ArrayList<>(items)),
                Collections.unmodifiableList(new ArrayList<>(items)));
    }

    private ItemsImpl(List<Item> elements, List<Item> inventory) {
        this.elements = elements;
        this.inventory = inventory;
        var teaseLib = teaseLib(elements, inventory);
        if (teaseLib != null) {
            this.random = teaseLib.random;
            this.logger = teaseLib.itemLogger;
        } else {
            this.random = null;
            this.logger = ItemLogger.None;
        }
    }

    private static TeaseLib teaseLib(List<Item> elements, List<Item> inventory) {
        return Stream.concat(elements.stream(), inventory.stream()).filter(Predicate.not(Item.NotFound::equals))
                .map(AbstractProxy::removeProxy).filter(ItemImpl.class::isInstance).map(ItemImpl.class::cast)
                .map(item -> item.teaseLib).findFirst().orElse(null);
    }

    @Override
    public ItemsImpl filter(Predicate<? super Item> predicate) {
        var filteredElements = elements.stream().filter(predicate).map(i -> (Item) i).toList();
        return new ItemsImpl(filteredElements, inventory);
    }

    @Override
    public boolean noneAvailable() {
        return log("noneAvailable", test(elements, Match.none, Item::isAvailable));
    }

    @Override
    public boolean anyAvailable() {
        return log("anyAvailable", test(elements, Match.any, Item::isAvailable));
    }

    @Override
    public boolean allAvailable() {
        if (elements.isEmpty()) {
            return false;
        }
        return log("allAvailable", test(elements, Match.all, Item::isAvailable));
    }

    @Override
    public boolean noneApplicable() {
        return log("noneApplicable", test(elements, Match.none, Item::canApply));
    }

    @Override
    public boolean anyApplicable() {
        return log("anyApplicable", test(elements, Match.any, Item::canApply));
    }

    @Override
    public boolean allApplicable() {
        if (elements.isEmpty()) {
            return false;
        }
        return log("allApplicable", test(elements, Match.all, Item::canApply));
    }

    @Override
    public boolean noneApplied() {
        return log("noneApplied", test(elements, Match.none, Item::applied));
    }

    @Override
    public boolean anyApplied() {
        return log("anyApplied", test(elements, Match.any, Item::applied));
    }

    @Override
    public boolean allApplied() {
        if (elements.isEmpty()) {
            return false;
        }
        return log("allApplied", test(elements, Match.all, Item::applied));
    }

    @Override
    public long removed(TimeUnit unit) {
        long removed = elements.stream()
                .map(AbstractProxy::undecorate)
                .map(item -> item.removed(unit))
                .reduce(Math::min).orElse(Long.MAX_VALUE);
        return log("removed", removed, unit);
    }

    @Override
    public boolean anyAre(Object... attributes) {
        return log("anyAre", test(elements, Match.any, item -> item.is(attributes)));
    }

    @Override
    public boolean allAre(Object... attributes) {
        if (elements.isEmpty()) {
            return false;
        }
        return log("allAre", test(elements, Match.all, item -> item.is(attributes)));
    }

    @Override
    public boolean anyExpired() {
        if (elements.isEmpty()) {
            return true;
        }
        return log("anyExpired", test(elements, Match.any, Item::expired));
    }

    @Override
    public boolean allExpired() {
        return log("allExpired", test(elements, Match.all, Item::expired));
    }

    private static class Match {
        static BiFunction<Stream<Item>, Predicate<Item>, Boolean> none = (s, p) -> s.noneMatch(p);
        static BiFunction<Stream<Item>, Predicate<Item>, Boolean> any = (s, p) -> s.anyMatch(p);
        static BiFunction<Stream<Item>, Predicate<Item>, Boolean> all = (s, p) -> s.allMatch(p);
    }

    private static boolean test(List<Item> elements, BiFunction<Stream<Item>, Predicate<Item>, Boolean> match, Predicate<Item> predicate) {
        return match.apply(elements.stream().map(AbstractProxy::undecorate), predicate);
    }

    private boolean log(String name, boolean value) {
        if (logger != null) {
            logger.log(elements, name, value);
        }
        return value;
    }

    private long log(String name, long value, TimeUnit unit) {
        if (logger != null) {
            logger.log(elements, name, value, unit);
        }
        return value;
    }

    @Override
    public ItemsImpl getAvailable() {
        ItemsImpl available = filterSkipSingleItemLogging(Item::isAvailable);
        logger.log("getAvailable", available);
        return available;
    }

    @Override
    public ItemsImpl getApplicable() {
        ItemsImpl applicable = filterSkipSingleItemLogging(Item::canApply);
        logger.log("getApplicable", applicable);
        return applicable;
    }

    @Override
    public ItemsImpl getApplied() {
        ItemsImpl applied = filterSkipSingleItemLogging(Item::applied);
        logger.log("getApplied", applied);
        return applied;
    }

    @Override
    public ItemsImpl getFree() {
        ItemsImpl free = filterSkipSingleItemLogging(Predicate.not(Item::applied));
        logger.log("getFree", free);
        return free;
    }

    @Override
    public ItemsImpl getExpired() {
        ItemsImpl expired = filterSkipSingleItemLogging(Item::expired);
        logger.log("getExpired", expired);
        return expired;
    }

    private ItemsImpl filterSkipSingleItemLogging(Predicate<? super Item> predicate) {
        return filter(item -> predicate.test(withoutLogging(item)));
    }

    private static Item withoutLogging(Item t) {
        return t instanceof ItemProxy ? AbstractProxy.itemImpl(t) : t;
    }

    /**
     * Return applied or random available item.
     * 
     * @return First item or {@link Item#NotFound}
     */
    @Override
    public Item get() {
        return getAppliedOrApplicableOrNotFound();
    }

    @Override
    public final Item get(int index) {
        return elements.get(index);
    }

    @Override
    public final Item get(Enum<?> item) {
        return item(QualifiedString.of(item));
    }

    @Override
    public final Item get(String item) {
        return item(QualifiedString.of(item));
    }

    private final Item item(QualifiedString item) {
        if (item.guid().isPresent()) {
            throw new UnsupportedOperationException("Support named items");
        }

        Item result = oneOfEachKind().stream().filter(element -> {
            return item.equals(AbstractProxy.removeProxy(element).kind());
        }).findFirst().orElse(Item.NotFound);
        return result;
    }

    private Item getAppliedOrApplicableOrNotFound() {
        List<Item> applied = getApplied().elements;
        if (!applied.isEmpty()) {
            return applied.get(0);
        } else {
            return getApplicableOrNotFound();
        }
    }

    private Item getApplicableOrNotFound() {
        List<Item> applicable = getApplicable().elements;
        if (!applicable.isEmpty()) {
            if (random != null) {
                return chooseItem(applicable);
            } else {
                return applicable.get(0);
            }
        } else if (!elements.isEmpty()) {
            if (random != null) {
                return chooseItem(elements);
            } else {
                return elements.get(0);
            }
        } else {
            return Item.NotFound;
        }
    }

    private Item chooseItem(List<Item> items) {
        return random.item(items);
    }

    /**
     * Get all items matching the supplied attributes. All items are returned, as a result the collection may contains
     * unavailable items.
     * 
     * @param attributes
     * @return Items that match all of the attributes.
     */
    @Override
    public final ItemsImpl matching(Enum<?>... attributes) {
        return matching(Arrays.asList(attributes));
    }

    @Override
    public final ItemsImpl matching(String... attributes) {
        return matching(Arrays.asList(attributes));
    }

    // TODO decide how to log matching/is

    private ItemsImpl matching(List<? extends Object> attributes) {
        ItemsImpl matchingItems;
        if (attributes.isEmpty()) {
            matchingItems = new ItemsImpl(this);
        } else {
            matchingItems = filterSkipSingleItemLogging(item -> item.is(attributes));
        }
        return matchingItems;
    }

    @Override
    public final ItemsImpl matchingAny(Enum<?>... attributes) {
        return matchingAny(Arrays.asList(attributes));
    }

    @Override
    public final ItemsImpl matchingAny(String... attributes) {
        return matchingAny(Arrays.asList(attributes));
    }

    // TODO decide how to log matching/is

    private ItemsImpl matchingAny(List<? extends Object> attributes) {
        ItemsImpl matchingItems;
        if (attributes.isEmpty()) {
            matchingItems = new ItemsImpl(this);
        } else {
            matchingItems = filterSkipSingleItemLogging(item -> isAny(item, attributes));
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

    @Override
    public boolean contains(Enum<?> item) {
        return containsImpl(QualifiedString.of(item));
    }

    @Override
    public boolean contains(String item) {
        return containsImpl(QualifiedString.of(item));
    }

    @Override
    public boolean contains(Item item) {
        return elements.contains(item);
    }

    @Override
    public boolean containsAny(Items items) {
        return !intersection(items).isEmpty();
    }

    @Override
    public Items intersection(Items items) {
        return new ItemsImpl(elements.stream().distinct().filter(items::contains).toList());
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
    @Override
    public ItemsImpl prefer(Enum<?>... attributes) {
        return preferredItems(attributes);
    }

    @Override
    public ItemsImpl prefer(String... attributes) {
        return preferredItems(attributes);
    }

    static class Preferred {
        private final List<Item> elements;
        private final java.util.Set<QualifiedString> found = new HashSet<>();
        private final List<Item> items = new ArrayList<>();
        private final Predicate<Item> matcher;

        public Preferred(List<Item> elements, Predicate<Item> matcher) {
            this.elements = elements;
            this.matcher = matcher;
        }

        // TODO Optimize items by number of matching attributes - best matching set with attribute coverage
        // e.g. all lockable, or all metal, use attribute order as priority

        public void addAvailableMatching() {
            for (Item item : elements) {
                // Add all matching, select set later for variance
                if (matcher.test(item) && item.isAvailable()) {
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
                if (missing(item) && !matcher.test(item)) {
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

    @Override
    public ItemsImpl avoid(Enum<?>... attributes) {
        return avoidedItems(attributes);
    }

    @Override
    public ItemsImpl avoid(String... attributes) {
        return avoidedItems(attributes);
    }

    // TODO decide how to handle logging for is(attributes), isAvailable8)

    private ItemsImpl preferredItems(Object[] attributes) {
        return preferredItems(item -> item.is(attributes));
    }

    private ItemsImpl avoidedItems(Object[] attributes) {
        return preferredItems(item -> !item.is(attributes));
    }

    private ItemsImpl preferredItems(Predicate<Item> matcher) {
        Preferred preferred = new Preferred(elements, matcher);
        preferred.addAvailableMatching();
        preferred.addAvailableNonMatching();
        preferred.addMissingMatching();
        preferred.addMissing();
        return new ItemsImpl(preferred.toList(), inventory);
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
    Varieties<Items.Set> varieties() {
        int variety = getVariety();
        Combinations<Item[]> combinations = Combinations.combinationsK(variety, toArray());
        return combinations.stream().map(Arrays::asList).filter(this::isVariety).map(items -> new ItemsImpl(items))
                .collect(Varieties.toVarieties());
    }

    private boolean isVariety(List<Item> combination) {
        return Varieties.isVariety(combination.stream().map(AbstractProxy::itemImpl).map(ItemImpl::kind).toList());
    }

    private int getVariety() {
        java.util.Set<String> types = new HashSet<>();
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
    static Items.Set best(Items.Set itemsA, Items.Set itemsB) {
        // TODO Improve attribute matching for applied items
        // - decide whether to consider preferred or matching attributes
        // - currently there is no attribute matching at all
        long a = itemsA.getAvailable().size();
        long b = itemsB.getAvailable().size();
        if (a == b) {
            // Count unique attributes found, then for each attribute add numberOfOccurences*count to rate sets higher
            a = attributesOfAvailable(itemsA).values().stream().reduce(Math::max).orElse(0L);
            b = attributesOfAvailable(itemsB).values().stream().reduce(Math::max).orElse(0L);
        }
        return a >= b ? itemsA : itemsB;
    }

    private static Map<QualifiedString, Long> attributesOfAvailable(Items items) {
        return items.stream().filter(Item::isAvailable).collect(groupingBy(QualifiedString::of, counting()));
    }

    @Override
    public State.Options apply() {
        if (elements.contains(Item.NotFound)) {
            Item.NotFound.apply();
        }

        // TODO apply without logging, log single line here, but collect options from proxy to preserve event handling
        List<State.Options> options = oneOfEachKind().stream().map(Item::apply).toList();
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

    @Override
    public void remove() {
        if (elements.contains(Item.NotFound)) {
            Item.NotFound.remove();
        }

        if (!elements.isEmpty()) {
            for (Item item : oneOfEachKind()) {
                item.remove();
            }
        }
    }

    public java.util.Collection<Item> oneOfEachKind() {
        List<Item> firstOfEachKind = new ArrayList<>();
        java.util.Set<QualifiedString> kinds = new HashSet<>();

        for (QualifiedString kind : this.elementSet()) {
            if (!kinds.contains(kind)) {
                kinds.add(kind);
                firstOfEachKind.add(getAppliedOrRandomAvailableOrNotFound(kind));
            }
        }

        return firstOfEachKind;
    }

    private Item getAppliedOrRandomAvailableOrNotFound(QualifiedString kind) {
        Item applied = getApplied().findFirst(kind);
        if (applied == Item.NotFound) {
            return availableOrInventoryItem(kind);
        }
        return applied;
    }

    private Item availableOrInventoryItem(QualifiedString kind) {
        var available = getAvailable().random(kind);
        if (available == Item.NotFound) {
            available = random(kind);
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
            return chooseItem(items);
        }
    }

    /**
     * Get a sublist just containing the requested items
     * 
     * @return A sublist containing the requested items, or {@link Item#NotFound} for any missing item.
     */
    @Override
    public ItemsImpl items(Enum<?>... anyItemOrAttribute) {
        return itemsImpl((Object[]) anyItemOrAttribute);
    }

    @Override
    public ItemsImpl items(String... anyItemOrAttribute) {
        return itemsImpl((Object[]) anyItemOrAttribute);
    }

    public ItemsImpl items(QualifiedString... anyItemOrAttribute) {
        return itemsImpl((Object[]) anyItemOrAttribute);
    }

    @Override
    public ItemsImpl items(Select.Statement... queries) {
        return new ItemsImpl(Stream.of(queries).map(query -> query.get(this)).flatMap(Items::stream).toList());
    }

    private ItemsImpl itemsImpl(Object... anyItemOrAttribute) {
        List<Item> items = new ArrayList<>();
        for (Item item : elements) {
            for (Object any : anyItemOrAttribute) {
                if (AbstractProxy.undecorate(item).is(any)) {
                    items.add(item);
                }
            }
        }
        return new ItemsImpl(items, inventory);
    }

    @Override
    public Iterator<Item> iterator() {
        return elements.iterator();
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public Stream<Item> stream() {
        return elements.stream();
    }

    public java.util.Set<QualifiedString> elementSet() {
        return qualifiedSet(elements);
    }

    java.util.Set<QualifiedString> inventorySet() {
        return qualifiedSet(inventory);
    }

    java.util.Set<QualifiedString> qualifiedSet(List<Item> items) {
        return items.stream().filter(AbstractProxy.class::isInstance).map(AbstractProxy::itemImpl)
                .map(ItemImpl::kind).collect(toCollection(LinkedHashSet::new));
    }

    @Override
    public List<Item> toList() {
        return Collections.unmodifiableList(elements);
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
        ItemsImpl other = (ItemsImpl) obj;
        if (elements == null) {
            if (other.elements != null)
                return false;
        } else if (!elements.equals(other.elements))
            return false;
        return true;
    }

    @Override
    public ItemsImpl orElsePrefer(Enum<?>... attributes) {
        return inventoryMatch(Item::isAvailable) ? this : new ItemsImpl(inventory).prefer(attributes);
    }

    @Override
    public ItemsImpl orElsePrefer(String... attributes) {
        return inventoryMatch(Item::isAvailable) ? this : new ItemsImpl(inventory).prefer(attributes);
    }

    @Override
    public ItemsImpl orElseMatching(Enum<?>... attributes) {
        return inventoryMatch(Item::isAvailable) ? this : new ItemsImpl(inventory).matching(attributes);
    }

    @Override
    public ItemsImpl orElseMatching(String... attributes) {
        return inventoryMatch(Item::isAvailable) ? this : new ItemsImpl(inventory).matching(attributes);
    }

    @Override
    public ItemsImpl orElse(Supplier<Items> items) {
        return inventoryMatch(Item::isAvailable) ? this : (ItemsImpl) items.get();
    }

    @Override
    public Items orElse(Query items) {
        return inventoryMatch(Item::isAvailable) ? this : (ItemsImpl) items.inventory();
    }

    private boolean inventoryMatch(Predicate<? super Item> predicate) {
        return allKindsMatch(inventorySet(), predicate);
    }

    private boolean allKindsMatch(java.util.Set<QualifiedString> kinds, Predicate<? super Item> predicate) {
        if (isEmpty()) {
            return false;
        }
        // all inventory items are tested for applied() and isAvailable() through Items.item()
        return kinds.stream().map(this::items).map(ItemsImpl::get).allMatch(predicate);
    }

    @Override
    public ItemsImpl without(Enum<?>... values) {
        return withoutImpl((Object[]) values);
    }

    @Override
    public ItemsImpl without(String... values) {
        return withoutImpl((Object[]) values);

    }

    private ItemsImpl withoutImpl(Object... values) {
        var qualifiedValues = Arrays.stream(values).map(QualifiedString::of).toList();
        List<Item> elementsWithout = filterSkipSingleItemLogging(
                item -> Arrays.stream(values).noneMatch(item::is)).toList();
        List<Item> inventoryWithout = filterSkipSingleItemLogging(
                item -> qualifiedValues.stream().noneMatch(
                        value -> {
                            return value.isItem()
                                    ? AbstractProxy.itemImpl(item).name.is(value)
                                    : AbstractProxy.itemImpl(item).name.kind().is(value);
                        }))
                                .toList();
        return new ItemsImpl(elementsWithout, inventoryWithout);
    }

    @Override
    public Collection to(Enum<?>... peers) {
        return withDefaultPeersImpl((Object[]) peers);
    }

    @Override
    public Collection to(String... peers) {
        return withDefaultPeersImpl((Object[]) peers);
    }

    private Collection withDefaultPeersImpl(Object... peers) {
        List<Item> items = elements.stream().map(item -> withAdditionalDefaultPeers(item, peers)).toList();
        return new ItemsImpl(items);
    }

    private static Item withAdditionalDefaultPeers(Item item, Object... additionalPeers) {
        return item.to(additionalPeers);
    }

    @Override
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
