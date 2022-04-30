/**
 * 
 */
package teaselib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import teaselib.core.ItemsImpl;
import teaselib.core.ItemsQueryImpl;
import teaselib.core.ResourceLoader;
import teaselib.core.Script;
import teaselib.core.ScriptRenderer;
import teaselib.core.TeaseLib;
import teaselib.core.state.ItemProxy;
import teaselib.core.state.StateProxy;
import teaselib.core.util.QualifiedString;
import teaselib.util.Item;
import teaselib.util.Items;
import teaselib.util.Select;
import teaselib.util.States;

/**
 * @author Citizen-Cane
 * 
 *         Implements object-oriented persistence.
 *
 */
public abstract class TeaseScriptPersistence extends Script {

    protected TeaseScriptPersistence(TeaseLib teaseLib, ResourceLoader resources, Actor actor, String namespace) {
        super(teaseLib, resources, actor, namespace);
    }

    protected TeaseScriptPersistence(Script script, Actor actor) {
        super(script, actor);
    }

    public static class Domain {
        final String name;
        final TeaseLib teaseLib;
        final String namespace;
        final ScriptRenderer scriptRenderer;

        public Domain(String name, Script script) {
            this.name = name;
            this.namespace = script.namespace;
            this.teaseLib = script.teaseLib;
            this.scriptRenderer = script.scriptRenderer;
        }

        public Items related(Items items) {
            return proxiesOf(teaseLib.relatedItems(name, items));
        }

        public <T extends Enum<?>> Item item(T value) {
            return new ItemProxy(namespace, teaseLib.item(name, value), scriptRenderer.events);
        }

        public Item item(String value) {
            Item item = teaseLib.item(name, value);
            if (item == Item.NotFound) {
                return item;
            } else {
                return new ItemProxy(namespace, item, scriptRenderer.events);
            }
        }

        public State state(Enum<?> value) {
            return new StateProxy(namespace, teaseLib.state(name, value), scriptRenderer.events);
        }

        public State state(String value) {
            return new StateProxy(namespace, teaseLib.state(name, value), scriptRenderer.events);
        }

        public final Items.Query items(Enum<?>[]... values) {
            Enum<?>[] array = Arrays.stream(values).flatMap(Arrays::stream).toArray(Enum<?>[]::new);
            return items(array);
        }

        public Items.Query items(String[]... values) {
            Enum<?>[] array = Arrays.stream(values).flatMap(Arrays::stream).toArray(Enum<?>[]::new);
            return items(array);
        }

        public Items.Query items(Enum<?>... values) {
            return new ItemsQueryImpl() {
                @Override
                public Items.Collection inventory() {
                    if (values.length > 0) {
                        return (Items.Collection) proxiesOf(teaseLib.items(name, (Object[]) values));
                    } else {
                        return (Items.Collection) Items.None;
                    }
                }
            };
        }

        public Items.Query items(String... values) {
            return new ItemsQueryImpl() {
                @Override
                public Items.Collection inventory() {
                    if (values.length > 0) {
                        return (Items.Collection) proxiesOf(teaseLib.items(name, (Object[]) values));
                    } else {
                        return (Items.Collection) Items.None;
                    }
                }
            };
        }

        public Items.Query items(Select.AbstractStatement statement) {
            return statement.get(items(statement.values));
        }

        public Items.Query items(Select.AbstractStatement... statements) {
            return new ItemsQueryImpl() {
                @Override
                public Items.Collection inventory() {
                    Function<? super Select.AbstractStatement, Items.Query> mapper = query -> query
                            .get(Domain.this.items(query.values));
                    List<Item> items = Arrays.stream(statements).map(mapper).map(Items.Query::inventory)
                            .flatMap(Items::stream).toList();
                    return new ItemsImpl(items);
                }

            };
        }

        @Override
        public String toString() {
            return name;
        }

        private Items proxiesOf(Items items) {
            List<Item> proxies = new ArrayList<>();
            for (Item item : items) {
                if (item instanceof ItemProxy) {
                    proxies.add(item);
                } else if (item == Item.NotFound) {
                    proxies.add(item);
                } else {
                    proxies.add(new ItemProxy(namespace, item, scriptRenderer.events));
                }
            }
            return new ItemsImpl(proxies);
        }
    }

    public final Domain defaultDomain = new Domain(TeaseLib.DefaultDomain, this);

    public Domain domain(Enum<?> domain) {
        return domain(QualifiedString.of(domain));
    }

    public Domain domain(QualifiedString domain) {
        return domain(domain.toString());
    }

    /**
     * Returns a script-local domain object that can be used to handle items in the specified domain.
     * 
     * @param domain
     * @return
     */
    public Domain domain(String domain) {
        return new Domain(domain, this);
    }

    /**
     * Get the item from an enumeration member
     * 
     * @param value
     *            The enumeration value to get the item for. The item is stored in the namespace of the script under its
     *            simple class name.
     * @return The item of the enumeration member
     */
    public <T extends Enum<?>> Item item(T value) {
        return defaultDomain.item(value);
    }

    public Item item(String value) {
        return defaultDomain.item(value);
    }

    public Items.Collection items(Item... items) {
        return new ItemsImpl(items);
    }

    public Items.Collection items(Items... items) {
        return new ItemsImpl(items);
    }

    public Items.Collection items(Items.Collection... items) {
        return new ItemsImpl(items);
    }

    public Items.Query items(Items.Query... items) {
        return new ItemsQueryImpl() {
            @Override
            public Items.Collection inventory() {
                return new ItemsImpl(Arrays.stream(items).map(Items.Query::inventory).flatMap(Items::stream).toList());
            }
        };
    }

    /**
     * Get items from a enumeration. This is different from toys and clothing in that toys and clothing is usually
     * maintained by the host, whereas script-related enumerations are handled by the script.
     * 
     * @param values
     * @return A list of items whose names are based on the enumeration members
     */
    public Items.Query items(Enum<?>... values) {
        return defaultDomain.items(values);
    }

    public Items.Query items(String... values) {
        return defaultDomain.items(values);
    }

    public Items.Query items(Enum<?>[]... values) {
        return defaultDomain.items(values);
    }

    public Items.Query items(String[]... values) {
        return defaultDomain.items(values);
    }

    public Items.Query items(Select.AbstractStatement statement) {
        return defaultDomain.items(statement);
    }

    public Items.Query items(Select.AbstractStatement... statements) {
        return defaultDomain.items(statements);
    }

    public States.Query states(Enum<?>... values) {
        return () -> new States(Arrays.stream(values).map(this::state).toList());
    }

    public TeaseLib.PersistentBoolean persistentBoolean(String name) {
        return teaseLib.new PersistentBoolean(TeaseLib.DefaultDomain, namespace, name);
    }

    public TeaseLib.PersistentBoolean persistentBoolean(Enum<?> name) {
        return teaseLib.new PersistentBoolean(TeaseLib.DefaultDomain, name);
    }

    public <T extends Enum<?>> TeaseLib.PersistentEnum<T> persistentEnum(String name, Class<T> enumClass) {
        return teaseLib.new PersistentEnum<>(TeaseLib.DefaultDomain, namespace, name, enumClass);
    }

    public <T extends Enum<?>> TeaseLib.PersistentEnum<T> persistentEnum(Enum<?> name, Class<T> enumClass) {
        return teaseLib.new PersistentEnum<>(TeaseLib.DefaultDomain, name, enumClass);
    }

    public <T extends Enum<?>> TeaseLib.PersistentEnum<T> persistentEnum(Class<T> enumClass) {
        return teaseLib.new PersistentEnum<>(TeaseLib.DefaultDomain, enumClass);
    }

    public TeaseLib.PersistentInteger persistentInteger(String name) {
        return teaseLib.new PersistentInteger(TeaseLib.DefaultDomain, namespace, name);
    }

    public TeaseLib.PersistentLong persistentLong(String name) {
        return teaseLib.new PersistentLong(TeaseLib.DefaultDomain, namespace, name);
    }

    public TeaseLib.PersistentFloat persistentFloat(String name) {
        return teaseLib.new PersistentFloat(TeaseLib.DefaultDomain, namespace, name);
    }

    public TeaseLib.PersistentString persistentString(String name) {
        return teaseLib.new PersistentString(TeaseLib.DefaultDomain, namespace, name);
    }

    public <T extends Enum<T>> TeaseLib.PersistentSequence<T> persistentSequence(String name, T[] values) {
        return teaseLib.new PersistentSequence<>(TeaseLib.DefaultDomain, namespace, name, values);
    }

    public State state(Enum<?> value) {
        return defaultDomain.state(value);
    }

    public State state(String value) {
        return defaultDomain.state(value);
    }
}
