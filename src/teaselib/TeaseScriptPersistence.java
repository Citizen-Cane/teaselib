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

    protected TeaseScriptPersistence(TeaseLib teaseLib,
            ResourceLoader resources, Actor actor, String namespace) {
        super(teaseLib, resources, actor, namespace);
    }

    protected TeaseScriptPersistence(TeaseScriptBase script, Actor actor) {
        super(script, actor);
    }

    /**
     * Get items from a enumeration. This is different from toys and clothing in
     * that toys and clothing is usually maintained by the host, whereas
     * script-related enumerations are handled by the script.
     * 
     * @param values
     * @return A list of items whose names are based on the enumeration members
     */
    public <T extends Enum<?>> Items<T> items(T... values) {
        if (values.length > 0) {
            final String namespace = this.namespace + "."
                    + values[0].getClass().getSimpleName();
            return teaseLib.items(TeaseLib.DefaultDomain, namespace, values);
        } else {
            return new Items<T>();
        }
    }

    public <T extends Enum<?>> Items<T> items(String domain, T... values) {
        if (values.length > 0) {
            String namespace = this.namespace + "."
                    + values[0].getClass().getSimpleName();
            return teaseLib.items(domain, namespace, values);
        } else {
            return new Items<T>();
        }
    }

    /**
     * Get the item from an enumeration member
     * 
     * @param value
     *            The enumeration value to get the item for. The item is stored
     *            in the namespace of the script under its simple class name.
     * @return The item of the enumeration member
     */
    public <T extends Enum<?>> Item<T> item(T value) {
        return teaseLib.item(TeaseLib.DefaultDomain, value);
    }

    public <T extends Enum<?>> Item<T> item(String domain, T value) {
        return teaseLib.item(domain, value);
    }

    public <T extends Enum<T>> Items<T> items(T[]... values) {
        Items<T> items = new Items<T>();
        for (T[] t : values) {
            items.addAll(items(TeaseLib.DefaultDomain, t));
        }
        return items;
    }

    public <T extends Enum<T>> Items<T> items(String domain, T[]... values) {
        Items<T> items = new Items<T>();
        for (T[] t : values) {
            items.addAll(items(domain, t));
        }
        return items;
    }

    /**
     * Retrieves an item in the namespace of the script.
     * 
     * @param value
     *            The item to retrieve.
     * @return The item that corresponds to the value.
     */
    public Item<String> item(String value) {
        return teaseLib.item(TeaseLib.DefaultDomain, namespace, value);
    }

    public Item<String> item(String domain, String value) {
        return teaseLib.item(domain, namespace, value);
    }

    public TeaseLib.PersistentBoolean persistentBoolean(String name) {
        return teaseLib.new PersistentBoolean(TeaseLib.DefaultDomain, namespace,
                name);
    }

    public TeaseLib.PersistentBoolean persistentBoolean(Enum<?> name) {
        return teaseLib.new PersistentBoolean(TeaseLib.DefaultDomain, name);
    }

    public <T extends Enum<?>> TeaseLib.PersistentEnum<T> persistentEnum(
            String name, T defaultValue) {
        return teaseLib.new PersistentEnum<T>(TeaseLib.DefaultDomain, namespace,
                name, defaultValue);
    }

    public <T extends Enum<?>> TeaseLib.PersistentEnum<T> persistentEnum(
            Enum<?> name, T defaultValue) {
        return teaseLib.new PersistentEnum<T>(TeaseLib.DefaultDomain, name,
                defaultValue);
    }

    public <T extends Enum<?>> TeaseLib.PersistentEnum<T> persistentEnum(
            T defaultValue) {
        return teaseLib.new PersistentEnum<T>(TeaseLib.DefaultDomain,
                defaultValue);
    }

    public TeaseLib.PersistentInteger persistentInteger(String name) {
        return teaseLib.new PersistentInteger(TeaseLib.DefaultDomain, namespace,
                name);
    }

    public TeaseLib.PersistentLong persistentLong(String name) {
        return teaseLib.new PersistentLong(TeaseLib.DefaultDomain, namespace,
                name);
    }

    public TeaseLib.PersistentFloat persistentFloat(String name) {
        return teaseLib.new PersistentFloat(TeaseLib.DefaultDomain, namespace,
                name);
    }

    public TeaseLib.PersistentString persistentString(String name) {
        return teaseLib.new PersistentString(TeaseLib.DefaultDomain, namespace,
                name);
    }

    public <T extends Enum<T>> TeaseLib.PersistentSequence<T> persistentSequence(
            String name, T[] values) {
        return teaseLib.new PersistentSequence<T>(TeaseLib.DefaultDomain,
                namespace, name, values);
    }

    public <T extends Enum<T>> State state(T item) {
        return teaseLib.state(item);
    }
}
