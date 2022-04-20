package teaselib.util;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import teaselib.State.Options;
import teaselib.core.util.QualifiedString;
import teaselib.util.Select.Statement;

public interface Items extends Iterable<Item> {

    public static final Items None = ItemsImpl.None;

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

    boolean isEmpty();

    int size();

    Item get(int index);

    Stream<Item> stream();

    boolean noneAvailable();

    boolean anyAvailable();

    boolean allAvailable();

    boolean noneApplicable();

    boolean anyApplicable();

    boolean allApplicable();

    boolean noneApplied();

    boolean anyApplied();

    boolean allApplied();

    boolean noneRemoved();

    boolean anyRemoved();

    boolean allRemoved();

    /**
     * Returns the time span since the last of the items has been removed.
     * 
     * @param unit
     * @return The time span since the last of the item have been removed, or 0 if any is still applied.
     */
    long removed(TimeUnit unit);

    boolean anyAre(Object... attributes);

    boolean allAre(Object... attributes);

    boolean anyExpired();

    boolean allExpired();

    Items getAvailable();

    Items getApplicable();

    Items getApplied();

    Items getFree();

    Items getExpired();

    boolean contains(Enum<?> item);

    boolean contains(String item);

    boolean contains(Item item);

    Items filter(Predicate<? super Item> predicate);

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
    Items prefer(Enum<?>... attributes);

    Items prefer(String... attributes);

    /**
     * Get all items matching the supplied attributes. All items are returned, as a result the collection may contains
     * unavailable items.
     * 
     * @param attributes
     * @return Items that match all of the attributes.
     */
    Items matching(Enum<?>... attributes);

    Items matching(String... attributes);

    Items matchingAny(Enum<?>... attributes);

    Items matchingAny(String... attributes);

    Items orElseItems(Enum<?>... items);

    Items orElseItems(String... items);

    Items orElsePrefer(Enum<?>... attributes);

    Items orElsePrefer(String... attributes);

    Items orElseMatching(Enum<?>... attributes);

    Items orElseMatching(String... attributes);

    Items orElse(Supplier<Items> items);

    Items without(Enum<?>... values);

    Items without(String... values);

    /**
     * Applies each item. If the list contains multiple items of the same kind, only the first of each kind is applied.
     * 
     */
    Options apply();

    /**
     * Applies each item to the given peers. If there are multiple items of the same kind, only the first of each kind
     * is applied. To apply to multiple instances, iterate over the elements and apply each with {@link Item#apply}
     * 
     */
    Options applyTo(Object... peers);

    void removeFrom(String... peers);

    void removeFrom(Enum<?>... peers);

    void remove();

    // TODO resolve return type to Collection or Set

    /**
     * Get a sublist just containing the requested items
     * 
     * @return A sublist containing the requested items, or {@link Item#NotFound} for any missing item.
     */
    Items items(Enum<?>... anyItemOrAttribute);

    Items items(String... anyItemOrAttribute);

    Items items(Statement query);

    Items intersection(Items items);

    //
    // TODO move methods to Collection

    /**
     * Return applied or random available item.
     * 
     * @return First item or {@link Item#NotFound}
     */
    Item get();

    // TODO private to Items.Query
    // Varieties<Items> varieties();

    public interface Collection {

    }

    //
    // TODO move to set

    /**
     * Return applied or first available item with the supplied value.
     * 
     * @return First item or {@link Item#NotFound}
     */
    Item get(Enum<?> item);

    /**
     * Return applied or first available item with the supplied value.
     * 
     * @return First item or {@link Item#NotFound}
     */
    Item item(Enum<?> item);

    Item item(String item);

    /**
     * return distinct item values.
     * 
     * @return
     */
    java.util.Set<QualifiedString> valueSet();

    public interface Set {

    }

    boolean containsAny(Items items);

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

}
