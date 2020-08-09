package teaselib.util;

import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface ItemsQuery extends Function<Items, Items> {

    public static ItemsQuery preferred(Enum<?> attributes) {
        return items -> items.prefer(attributes);
    }

    public static ItemsQuery matching(Enum<?> attributes) {
        return items -> items.matching(attributes);
    }

    public static ItemsQuery applied() {
        return Items::getApplied;
    }

    public static ItemsQuery available() {
        return Items::getAvailable;
    }

    public static ItemsQuery appliapplicableQuery() {
        return Items::getApplicable;
    }

    interface Result extends Supplier<Items> {
        //
    }

    public static Result select(ItemsQuery.Result items, ItemsQuery... statements) {
        return () -> {
            Items selection = items.get();
            for (ItemsQuery itemsQuery : statements) {
                selection = itemsQuery.apply(selection);
            }
            return selection;
        };
    }

}
