/**
 * 
 */
package teaselib;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import teaselib.core.ResourceLoader;
import teaselib.core.Script;
import teaselib.core.TeaseLib;
import teaselib.core.state.AbstractProxy;
import teaselib.core.state.ItemProxy;
import teaselib.core.state.StateProxy;
import teaselib.core.util.QualifiedEnum;
import teaselib.core.util.QualifiedItem;
import teaselib.util.Item;
import teaselib.util.ItemImpl;
import teaselib.util.Items;
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

    public class Domain {
        final String name;

        public Domain(String name) {
            this.name = name;
        }

        public Item item(Item item) {
            if (item == Item.NotFound) {
                return item;
            } else {
                ItemImpl itemImpl = AbstractProxy.itemImpl(item);
                return new ItemProxy(namespace, getItemByGuid(itemImpl), scriptRenderer.events);
            }
        }

        public Items related(Items items) {
            return proxiesOf(teaseLib.relatedItems(name, items));
        }

        private Item getItemByGuid(ItemImpl item) {
            return teaseLib.getByGuid(name, item.value, item.guid.name());
        }

        public Items items(Enum<?>... values) {
            if (values.length > 0) {
                return proxiesOf(teaseLib.items(name, (Object[]) values));
            } else {
                return Items.None;
            }
        }

        public Items items(String... values) {
            if (values.length > 0) {
                return proxiesOf(teaseLib.items(name, (Object[]) values));
            } else {
                return Items.None;
            }
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

        @SafeVarargs
        public final <T extends Enum<?>> Items items(T[]... values) {
            List<Item> items = new ArrayList<>();
            for (T[] s : values) {
                Items.addTo(items, items(s));
            }
            return new Items(items);
        }

        public Items items(String[]... values) {
            List<Item> items = new ArrayList<>();
            for (String[] s : values) {
                Items.addTo(items, items(s));
            }
            return new Items(items);
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
            return new Items(proxies);
        }
    }

    public final Domain defaultDomain = new Domain(TeaseLib.DefaultDomain);

    public Domain domain(Enum<?> domain) {
        return domain(new QualifiedEnum(domain));
    }

    public Domain domain(QualifiedItem domain) {
        return domain(domain.toString());
    }

    public Domain domain(String domain) {
        return new Domain(domain);
    }

    /**
     * Get items from a enumeration. This is different from toys and clothing in that toys and clothing is usually
     * maintained by the host, whereas script-related enumerations are handled by the script.
     * 
     * @param values
     * @return A list of items whose names are based on the enumeration members
     */
    public Items items(Enum<?>... values) {
        return defaultDomain.items(values);
    }

    public Items items(String... values) {
        return defaultDomain.items(values);
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

    public Items items(Enum<?>[]... values) {
        return defaultDomain.items(values);
    }

    public Items items(String[]... values) {
        return defaultDomain.items(values);
    }

    public Items.Query query(Enum<?>... values) {
        return () -> defaultDomain.items(values);
    }

    public States.Query states(Enum<?>... values) {
        return () -> new States(Arrays.stream(values).map(this::state).collect(toList()));
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
