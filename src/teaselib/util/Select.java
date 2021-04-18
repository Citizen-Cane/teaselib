package teaselib.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

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
        return () -> {
            Items result = items.get();
            for (Clause<Items> statement : statements) {
                result = statement.apply(result);
            }
            return result;
        };
    }

    @SafeVarargs
    public static States.Query select(States.Query states, Clause<States>... statements) {
        return () -> {
            States result = states.get();
            for (Clause<States> statement : statements) {
                result = statement.apply(result);
            }
            return result;
        };
    }

    public static Statement items(Enum<?>... values) {
        return new Statement(values, Collections.emptyList());
    }

    public abstract static class AbstractStatement {

        public final Enum<?>[] values;
        final List<Clause<Items>> clauses;

        AbstractStatement(Enum<?>[] values, List<Clause<Items>> clauses) {
            super();
            this.values = values;
            this.clauses = clauses;
        }

        public Items.Query get(Items.Query items) {
            @SuppressWarnings("unchecked")
            Clause<Items>[] array = new Clause[clauses.size()];
            return select(items, clauses.toArray(array));
        }

        List<Clause<Items>> gather(Enum<?>... items) {
            BiFunction<Items, Enum<?>[], Items> selector = (i, v) -> i.items(items);
            Clause<Items> statement = i -> selector.apply(i, values);
            List<Clause<Items>> clause = new ArrayList<>(clauses);
            clause.add(statement);
            return clause;
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
                List<Clause<Items>> where = new ArrayList<>(Arrays.asList(statements));
                return new Additional(values, where);
            }

            public Additional and(BiFunction<Items, Enum<?>[], Items> selector, Enum<?>... attributes) {
                Clause<Items> statement = items -> selector.apply(items, attributes);
                List<Clause<Items>> clause = new ArrayList<>(clauses);
                clause.add(statement);
                return new Additional(values, clause);
            }

            public Additional items(Enum<?>... items) {
                return new Additional(values, gather(items));
            }

        }
    }

}
