package teaselib.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import teaselib.core.state.AbstractProxy;
import teaselib.core.util.QualifiedString;
import teaselib.util.Items.Query;

// TODO package private - requires moving a bunch of other classes
public abstract class ItemsQueryImpl implements Items.Query {

    public static final Query None = new ItemsQueryImpl() {
        @Override
        public Items inventory() {
            return Items.None;
        }
    };

    @Override
    public Query prefer(Enum<?>... values) {
        return query(items -> items.prefer(values));
    }

    @Override
    public Query prefer(String... values) {
        return query(items -> items.prefer(values));
    }

    @Override
    public Query matching(Enum<?>... values) {
        return query(items -> items.matching(values));
    }

    @Override
    public Query matching(String... values) {
        return query(items -> items.matching(values));
    }

    @Override
    public Query matchingAny(Enum<?>... values) {
        return query(items -> items.matchingAny(values));
    }

    @Override
    public Query matchingAny(String... values) {
        return query(items -> items.matchingAny(values));
    }

    @Override
    public Query without(Enum<?>... values) {
        return query(items -> items.without(values));
    }

    @Override
    public Query without(String... values) {
        return query(items -> items.without(values));
    }

    @Override
    public Query orElseItems(Enum<?>... values) {
        return query(items -> items.orElseItems(values));
    }

    @Override
    public Query orElseItems(String... values) {
        return query(items -> items.orElseItems(values));
    }

    @Override
    public Query orElsePrefer(Enum<?>... values) {
        return query(items -> items.orElsePrefer(values));
    }

    @Override
    public Query orElsePrefer(String... values) {
        return query(items -> items.orElsePrefer(values));
    }

    @Override
    public Query orElseMatching(Enum<?>... values) {
        return query(items -> items.orElseMatching(values));
    }

    @Override
    public Query orElseMatching(String... values) {
        return query(items -> items.orElseMatching(values));
    }

    @Override
    public Query orElse(Items.Query otherItems) {
        return anyAvailable() ? this : otherItems;
    }

    private Query query(Function<Items, Items> function) {
        return new ItemsQueryImpl() {
            @Override
            public Items inventory() {
                return function.apply(ItemsQueryImpl.this.inventory());
            }
        };
    }

    @Override
    public boolean noneApplied() {
        return inventory().noneApplied();
    }

    @Override
    public boolean noneAvailable() {
        return inventory().noneAvailable();
    }

    @Override
    public boolean noneApplicable() {
        return inventory().noneApplicable();
    }

    @Override
    public boolean anyApplied() {
        return inventory().anyApplied();
    }

    @Override
    public boolean anyAvailable() {
        return inventory().anyAvailable();
    }

    @Override
    public boolean anyApplicable() {
        return inventory().anyApplicable();
    }

    @Override
    public boolean allApplied() {
        return allKindsMatch(Item::applied);
    }

    @Override
    public boolean allApplicable() {
        return allKindsMatch(Item::canApply);
    }

    @Override
    public boolean allAvailable() {
        return allKindsMatch(Item::isAvailable);
    }

    private boolean allKindsMatch(Predicate<? super Item> predicate) {
        Items items = inventory();
        // all inventory items are tested for applied() and isAvailable() through Items.item()
        return items.valueSet().stream().map(QualifiedString::toString).map(items::item).allMatch(predicate);
    }

    @Override
    public Item item() {
        return inventory().get();
    }

    @Override
    public Items getApplicableSet() {
        // TODO Split Items into ItemsImpl & interfaces Items & ItemSet
        // -> return ItemSet here, for inventory queries Items - return always ItemsImpl
        //
        // TODO match best set according to prefer() and matching already applied items
        // TODO review item(Enum<?>) to return random item instead of first
        Map<QualifiedString, Item> items = new LinkedHashMap<>();
        Iterator<Item> iterator = inventory().iterator();
        while (iterator.hasNext()) {
            var item = iterator.next();
            ItemImpl impl = AbstractProxy.removeProxy(item);
            if (item.canApply() && !items.containsKey(impl.kind())) {
                items.put(impl.kind(), impl);
            }
        }
        return new ItemsImpl(new ArrayList<>(items.values()));
    }

    @Override
    public Items getApplied() {
        return inventory().getApplied();
    }

    @Override
    public Items getAvailable() {
        return inventory().getAvailable();
    }

    @Override
    public Items getApplicable() {
        return inventory().getApplicable();
    }

    @Override
    abstract public Items inventory();
}