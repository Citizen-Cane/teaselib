/**
 * 
 */
package teaselib;

import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;
import teaselib.core.TeaseScriptBase;
import teaselib.util.Item;
import teaselib.util.Items;

/**
 * @author Citizen-Cane
 * 
 *         Implements object-oriented persistence.
 *
 */
public abstract class TeaseScriptPersistence extends TeaseScriptBase {

    protected TeaseScriptPersistence(TeaseLib teaseLib, ResourceLoader resources, Actor actor,
            String namespace) {
        super(teaseLib, resources, actor, namespace);
    }

    protected TeaseScriptPersistence(TeaseScriptBase script, Actor actor) {
        super(script, actor);
    }

    public class Domain {
        final String domain;

        public Domain(String domain) {
            this.domain = domain;
        }

        public <T extends Enum<?>> Items items(T... values) {
            if (values.length > 0) {
                return teaseLib.items(domain, values);
            } else {
                return Items.None;
            }
        }

        public Items items(String... values) {
            if (values.length > 0) {
                return teaseLib.items(domain, values);
            } else {
                return Items.None;
            }
        }

        public <T extends Enum<?>> Item item(T value) {
            return teaseLib.item(domain, value);
        }

        public Item item(String value) {
            return teaseLib.item(domain, value);
        }

        public <T extends Enum<?>> Items items(T[]... values) {
            Items items = new Items();
            for (T[] s : values) {
                items.addAll(items(s));
            }
            return items;
        }

        public Items items(String[]... values) {
            Items items = new Items();
            for (String[] s : values) {
                items.addAll(items(s));
            }
            return items;
        }
    }

    final Domain defaultDomain = new Domain(TeaseLib.DefaultDomain);

    public Domain domain(String domain) {
        return new Domain(domain);
    }

    /**
     * Get items from a enumeration. This is different from toys and clothing in
     * that toys and clothing is usually maintained by the host, whereas
     * script-related enumerations are handled by the script.
     * 
     * @param values
     * @return A list of items whose names are based on the enumeration members
     */
    public <T extends Enum<?>> Items items(T... values) {
        return defaultDomain.items(values);
    }

    public Items items(String... values) {
        return defaultDomain.items(values);
    }

    /**
     * Get the item from an enumeration member
     * 
     * @param value
     *            The enumeration value to get the item for. The item is stored
     *            in the namespace of the script under its simple class name.
     * @return The item of the enumeration member
     */
    public <T extends Enum<?>> Item item(T value) {
        return defaultDomain.item(value);
    }

    public Item item(String value) {
        return defaultDomain.item(value);
    }

    public <T extends Enum<?>> Items items(T[]... values) {
        return defaultDomain.items(values);
    }

    public Items items(String[]... values) {
        return defaultDomain.items(values);
    }

    public TeaseLib.PersistentBoolean persistentBoolean(String name) {
        return teaseLib.new PersistentBoolean(TeaseLib.DefaultDomain, namespace, name);
    }

    public TeaseLib.PersistentBoolean persistentBoolean(Enum<?> name) {
        return teaseLib.new PersistentBoolean(TeaseLib.DefaultDomain, name);
    }

    public <T extends Enum<?>> TeaseLib.PersistentEnum<T> persistentEnum(String name,
            Class<T> enumClass) {
        return teaseLib.new PersistentEnum<T>(TeaseLib.DefaultDomain, namespace, name, enumClass);
    }

    public <T extends Enum<?>> TeaseLib.PersistentEnum<T> persistentEnum(Enum<?> name,
            Class<T> enumClass) {
        return teaseLib.new PersistentEnum<T>(TeaseLib.DefaultDomain, name, enumClass);
    }

    public <T extends Enum<?>> TeaseLib.PersistentEnum<T> persistentEnum(Class<T> enumClass) {
        return teaseLib.new PersistentEnum<T>(TeaseLib.DefaultDomain, enumClass);
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

    public <T extends Enum<T>> TeaseLib.PersistentSequence<T> persistentSequence(String name,
            T[] values) {
        return teaseLib.new PersistentSequence<T>(TeaseLib.DefaultDomain, namespace, name, values);
    }

    public <T extends Enum<?>> State state(T item) {
        return teaseLib.state(TeaseLib.DefaultDomain, item);
    }

    public State state(String item) {
        return teaseLib.state(TeaseLib.DefaultDomain, item);
    }
}
