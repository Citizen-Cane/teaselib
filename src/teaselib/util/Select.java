package teaselib.util;

import java.util.function.Function;

public interface Select {

    interface Statement<T> extends Function<T, T> {
        //
    }

    public static Statement<Items> preferred(Enum<?> attributes) {
        return items -> items.prefer(attributes);
    }

    public static Statement<Items> matching(Enum<?> attributes) {
        return items -> items.matching(attributes);
    }

    @SafeVarargs
    public static Items.Query select(Items.Query items, Statement<Items>... statements) {
        return () -> {
            Items query = items.get();
            for (Statement<Items> statement : statements) {
                query = statement.apply(query);
            }
            return query;
        };
    }

    @SafeVarargs
    public static States.Query select(States.Query states, Statement<States>... statements) {
        return () -> {
            States query = states.get();
            for (Statement<States> statement : statements) {
                query = statement.apply(query);
            }
            return query;
        };
    }

}
