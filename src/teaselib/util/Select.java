package teaselib.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import teaselib.core.ItemsImpl;
import teaselib.core.ItemsQueryImpl;

public class Select {

    @FunctionalInterface
    public interface Clause<T> extends Function<T, T> {
        // Tag interface
    }

    public static Clause<Items> preferred(Enum<?> attributes) {
        return items -> items.prefer(attributes);
    }

    public static Clause<Items> matching(Enum<?> attributes) {
        return items -> items.matching(attributes);
    }

    @SafeVarargs
    public static Items.Query select(Items.Query items, Clause<Items>... statements) {
        return select(items, Arrays.asList(statements));
    }

    public static Items.Query select(Items.Query items, List<Clause<Items>> statements) {
        Items.Query selected = new ItemsQueryImpl() {
            @Override
            public ItemsImpl inventory() {
                return (ItemsImpl) select(items.inventory(), statements);
            }

        };
        return selected;
    }

    public static Items select(Items items, List<Clause<Items>> statements) {
        var result = items;
        for (Clause<Items> statement : statements) {
            result = statement.apply(result);
        }
        return result;
    }

    @SafeVarargs
    public static States.Query select(States.Query states, Clause<States>... statements) {
        return new States.QueryImpl(() -> {
            var result = states.get();
            for (Clause<States> statement : statements) {
                result = statement.apply(result);
            }
            return result;
        });
    }

    public static Statement items(Enum<?>[]... values) {
        Enum<?>[] all = Arrays.stream(values).flatMap(Arrays::stream).toArray(Enum<?>[]::new);
        return items(all);
    }

    public static Statement items(Enum<?>... values) {
        return new Statement(values, Collections.singletonList(items -> items.matchingAny(values)));
    }

    public abstract static class AbstractStatement {

        // TODO package private Statement.values

        public final Enum<?>[] values;
        final List<Clause<Items>> clauses;

        AbstractStatement(Enum<?>[] values, List<Clause<Items>> clauses) {
            super();
            this.values = values;
            this.clauses = clauses;
        }

        public Items get(Items items) {
            return select(items, clauses);
        }

        public Items.Query get(Items.Query items) {
            return select(items, clauses);
        }

        static List<Clause<Items>> gather(Enum<?>... items) {
            return Collections.singletonList(query -> query.items(items));
        }

    }

    public static Statement statement(AbstractStatement statement) {
        return new Statement(statement);
    }

    public static class Statement extends AbstractStatement {

        Statement(Enum<?>[] values, List<Clause<Items>> clauses) {
            super(values, clauses);
        }

        public Statement(AbstractStatement statement) {
            super(statement.values, statement.clauses);
        }

        @SafeVarargs
        public final Statement where(Clause<Items>... statements) {
            List<Clause<Items>> where = new ArrayList<>(Arrays.asList(statements));
            return new Statement(values, where);
        }

        public Additional where(BiFunction<Items, Enum<?>[], Items> selector, Enum<?>... attributes) {
            Clause<Items> statement = items -> selector.apply(items, attributes);
            List<Clause<Items>> clause = new ArrayList<>(clauses);
            clause.add(statement);
            return new Additional(values, clause);
        }

        public Statement items(Enum<?>... items) {
            return new Statement(values, gather(items));
        }

        public static class Additional extends AbstractStatement {

            Additional(Enum<?>[] values, List<Clause<Items>> clauses) {
                super(values, clauses);
            }

            @SafeVarargs
            public final Additional and(Clause<Items>... statements) {
                List<Clause<Items>> and = new ArrayList<>(clauses);
                and.addAll(Arrays.asList(statements));
                return new Additional(values, and);
            }

            public Additional and(BiFunction<Items, Enum<?>[], Items> selector, Enum<?>... attributes) {
                List<Clause<Items>> and = new ArrayList<>(clauses);
                and.add(items -> selector.apply(items, attributes));
                return new Additional(values, and);
            }

            public Additional items(Enum<?>... items) {
                return new Additional(values, gather(items));
            }

        }
    }

}
