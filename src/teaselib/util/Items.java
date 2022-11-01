package teaselib.util;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import teaselib.State.Options;
import teaselib.core.ItemsImpl;

public interface Items extends Iterable<Item>, Inventory {

    public static final Items None = ItemsImpl.None;

    public interface Query extends Inventory {

        Query prefer(Enum<?>... values);

        Query prefer(String... values);

        Query avoid(Enum<?>... attributes);

        Query avoid(String... attributes);

        Query matching(Enum<?>... values);

        Query matching(String... values);

        Query matchingAny(Enum<?>... values);

        Query matchingAny(String... values);

        /**
         * Remove items that match any of the value arguments. If a value matches the item name or kind, the item will
         * also be removed from this ItemQuery's inventory. As a result, orElse() will not replace the query, because
         * the elements are complete, e.g. their value set matches the inventory set of the originally requested items.
         * 
         * @param values
         *            values for which matching items should be removed.
         * @return A new Items collection with the remaining items.
         */
        Query without(Enum<?>... values);

        /**
         * Remove items that match any of the value arguments. If a value matches the item name or kind, the item will
         * also be removed from this ItemQuery's inventory. As a result, orElse() will not replace the query, because
         * the elements are complete, e.g. their value set matches the inventory set of the originally requested items.
         * 
         * @param values
         *            values for which matching items should be removed.
         * @return A new Items collection with the remaining items.
         */
        Query without(String... values);

        Query orElseItems(Enum<?>... items);

        Query orElseItems(String... items);

        Query orElsePrefer(Enum<?>... attributes);

        Query orElsePrefer(String... attributes);

        Query orElseMatching(Enum<?>... attributes);

        Query orElseMatching(String... attributes);

        /**
         * Specifies a different query when the current query does not yield all requested kinds. REsults are always
         * replaced, never extended.
         * <p>
         * To extend results, better use prefer(), or to start over, use orElsePtefer().
         * 
         * @param items
         * @return
         */
        Query orElse(Items.Query items);

        Query filter(Predicate<? super Item> predicate);

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
        Items.Set getApplicableSet();

        /**
         * Get all items that are currently applied.
         * 
         * @return All applied items.
         */
        Items.Collection getApplied();

        /**
         * Get all items that are currently available.
         * 
         * @return All available items.
         */
        Items.Collection getAvailable();

        /**
         * Get all items that are currently applied.
         * 
         * @return All applied items.
         */
        Items.Collection getApplicable();

        /**
         * Get all items that are defined in the inventory. This also includes items that are defined but not available.
         * 
         * @return All defined items in the inventory.
         */
        Items.Collection inventory();
    }

    boolean isEmpty();

    int size();

    Item get(int index);

    Stream<Item> stream();

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

    boolean contains(Enum<?> item);

    boolean contains(String item);

    boolean contains(Item item);

    boolean containsAny(Items items);

    Items filter(Predicate<? super Item> predicate);

    List<Item> toList();

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

    Items avoid(Enum<?>... attributes);

    Items avoid(String... attributes);

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

    /**
     * Remove items that match any of the value arguments. If value matches the item name or kind, the item will also be
     * removed from this Items instance's inventory. As a result, orElse() will not replace the selected items, because
     * the elements are complete, e.g. their value set matches the inventory set of the originally requested items.
     * 
     * @param values
     *            values for which matching items should be removed.
     * @return A new Items collection with the remaining items.
     */
    Items without(Enum<?>... values);

    /**
     * Remove items that match any of the value arguments. If value matches the item name or kind, the item will also be
     * removed from this Items instance's inventory. As a result, orElse() will not replace the selected items, because
     * the elements are complete, e.g. their value set matches the inventory set of the originally requested items.
     * 
     * @param values
     *            values for which matching items should be removed.
     * @return A new Items collection with the remaining items.
     */
    Items without(String... values);

    Items orElseItems(Enum<?>... items);

    Items orElseItems(String... items);

    /**
     * When the current query contains all items, return the current query. Otherwise return a new query preferring
     * inventory items with the specified attributes.
     * 
     * @param attributes
     *            Preferred attributes
     * @return New query selecting inventory items with the specified attributes.
     * 
     */
    Items orElsePrefer(Enum<?>... attributes);

    /**
     * When the current query contains all items, return the current query. Otherwise return a new query preferring
     * inventory items with the specified attributes.
     * 
     * @param attributes
     *            Preferred attributes
     * @return New query selecting inventory items with the specified attributes.
     * 
     */
    Items orElsePrefer(String... attributes);

    Items orElseMatching(Enum<?>... attributes);

    Items orElseMatching(String... attributes);

    Items orElse(Supplier<Items> items);

    /**
     * Applies each item. If the list contains multiple items of the same kind, only the first of each kind is applied.
     * 
     */
    Options apply();

    void remove();

    /**
     * Get a sublist just containing the requested items
     * 
     * @return A sublist containing the requested items, or {@link Item#NotFound} for any missing item.
     */
    Items items(Enum<?>... anyItemOrAttribute);

    Items items(String... anyItemOrAttribute);

    Items items(Select.Statement... queries);

    Items intersection(Items items);

    Items getAvailable();

    Items getApplicable();

    Items getApplied();

    Items getFree();

    Items getExpired();

    public interface Collection extends Items {

        public static final Items.Set Empty = ItemsImpl.None;

        @Override
        Collection filter(Predicate<? super Item> predicate);

        @Override
        Collection prefer(Enum<?>... attributes);

        @Override
        Collection prefer(String... attributes);

        @Override
        Collection avoid(Enum<?>... attributes);

        @Override
        Collection avoid(String... attributes);

        @Override
        Collection matching(Enum<?>... attributes);

        @Override
        Collection matching(String... attributes);

        @Override
        Collection matchingAny(Enum<?>... attributes);

        @Override
        Collection matchingAny(String... attributes);

        @Override
        Collection without(Enum<?>... values);

        @Override
        Collection without(String... values);

        @Override
        Collection orElseItems(Enum<?>... items);

        @Override
        Collection orElseItems(String... items);

        @Override
        Collection orElsePrefer(Enum<?>... attributes);

        @Override
        Collection orElsePrefer(String... attributes);

        @Override
        Collection orElseMatching(Enum<?>... attributes);

        @Override
        Collection orElseMatching(String... attributes);

        @Override
        Collection orElse(Supplier<Items> items);

        /**
         * Return applied or random applicable or available item.
         * 
         * @return First item or {@link Item#NotFound}
         */
        Item get();

        @Override
        Collection getAvailable();

        @Override
        Collection getApplicable();

        @Override
        Collection getApplied();

        @Override
        Collection getFree();

        @Override
        Collection getExpired();

        @Override
        Collection items(Enum<?>... items);

        @Override
        Collection items(String... items);

        /**
         * Produce similar items with additional default peers. The similar item will extend the original item with
         * additional default peers, e.g. when it's applied, the original item will be applied to its original default
         * peers, plus the additional peers.
         * 
         * @param additionalPeers
         *            Additional default peers for the similar items.
         * @return A new items collection with extensions of the original items, with additional default peers to be
         *         applied. The similar items are the same entity as the original items, e.g. when applied, the original
         *         item will also change its state to applied.
         */
        Collection to(Enum<?>... additionalPeers);

        Collection to(String... additionalPeers);
    }

    public interface Set extends Items {

        public static final Items.Set EmptySet = ItemsImpl.None;

        @Override
        Set filter(Predicate<? super Item> predicate);

        @Override
        Set prefer(Enum<?>... attributes);

        @Override
        Set prefer(String... attributes);

        @Override
        Set avoid(Enum<?>... attributes);

        @Override
        Set avoid(String... attributes);

        @Override
        Set matching(Enum<?>... attributes);

        @Override
        Set matching(String... attributes);

        @Override
        Set matchingAny(Enum<?>... attributes);

        @Override
        Set matchingAny(String... attributes);

        @Override
        Set without(Enum<?>... values);

        @Override
        Set without(String... values);

        @Override
        Set orElseItems(Enum<?>... items);

        @Override
        Set orElseItems(String... items);

        @Override
        Set orElsePrefer(Enum<?>... attributes);

        @Override
        Set orElsePrefer(String... attributes);

        @Override
        Set orElseMatching(Enum<?>... attributes);

        @Override
        Set orElseMatching(String... attributes);

        @Override
        Set orElse(Supplier<Items> items);

        @Override
        Set getAvailable();

        @Override
        Set getApplicable();

        @Override
        Set getApplied();

        @Override
        Set getFree();

        @Override
        Set getExpired();

        /**
         * Return applied or random applicable or available item matching the supplied value.
         * 
         * @return Item instance or {@link Item#NotFound}
         */
        Item get(Enum<?> item);

        Item get(String item);

        @Override
        Items.Set items(Enum<?>... items);

        @Override
        Items.Set items(String... items);
    }

}
