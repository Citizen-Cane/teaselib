/**
 * 
 */
package teaselib;

import teaselib.core.ResourceLoader;
import teaselib.core.TeaseScriptBase;
import teaselib.util.Item;
import teaselib.util.Items;

/**
 * @author someone
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

    public <T extends Enum<?>> Item<T> toy(T item) {
        return teaseLib.getToy(item);
    }

    public <C extends Enum<?>> Item<C> clothing(Object wearer, C item) {
        return teaseLib.getClothing(wearer, item);
    }

    public <T> Item<T> toy(T item) {
        return teaseLib.getToy(item);
    }

    public <C> Item<C> clothing(Object wearer, C item) {
        return teaseLib.getClothing(wearer, item);
    }

    public <T extends Enum<?>> Items<T> toys(T... toys) {
        Items<T> items = new Items<T>();
        for (T toy : toys) {
            Item<T> item = teaseLib.getToy(toy);
            items.add(item);
        }
        return items;
    }

    public <T extends Enum<?>> Items<T> toys(T[]... toys) {
        Items<T> items = new Items<T>();
        for (T[] selection : toys) {
            items.addAll(toys(selection));
        }
        return items;
    }

    public <T> Items<T> toys(T... toys) {
        Items<T> items = new Items<T>();
        for (T toy : toys) {
            Item<T> item = teaseLib.getToy(toy);
            items.add(item);
        }
        return items;
    }

    public <T> Items<T> toys(T[]... toys) {
        Items<T> items = new Items<T>();
        for (T[] selection : toys) {
            items.addAll(toys(selection));
        }
        return items;
    }

    public <C extends Enum<C>> Items<C> clothes(Object wearer, C... clothes) {
        Items<C> items = new Items<C>();
        for (C clothing : clothes) {
            Item<C> item = teaseLib.getClothing(wearer, clothing);
            items.add(item);
        }
        return items;
    }

    public <C extends Enum<C>> Items<C> clothes(Object wearer,
            C[]... clothing) {
        Items<C> items = new Items<C>();
        for (C[] selection : clothing) {
            items.addAll(clothes(wearer, selection));
        }
        return items;
    }

    public <C> Items<C> clothes(Object wearer, C... clothes) {
        Items<C> items = new Items<C>();
        for (C clothing : clothes) {
            Item<C> item = teaseLib.getClothing(wearer, clothing);
            items.add(item);
        }
        return items;
    }

    public <C> Items<C> clothes(Object wearer, C[]... clothing) {
        Items<C> items = new Items<C>();
        for (C[] selection : clothing) {
            items.addAll(clothes(wearer, selection));
        }
        return items;
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
            return teaseLib.items(namespace, values);
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
        final String namespace = this.namespace;
        return teaseLib.item(namespace, value);
    }

    /**
     * Retrieves an item in the namespace of the script.
     * 
     * @param value
     *            The item to retrieve.
     * @return The item that corresponds to the value.
     */
    public <T> Item<T> item(T value) {
        final String namespace = this.namespace;
        return teaseLib.item(namespace, value);
    }

    public void clear(String name) {
        teaseLib.clear(namespace, name);
    }

    public void clear(Enum<?> name) {
        teaseLib.clear(namespace, name);
    }

    public void set(Enum<?> name, boolean value) {
        teaseLib.set(namespace, name, value);
    }

    public void set(Enum<?> name, int value) {
        teaseLib.set(namespace, name, value);
    }

    public void set(Enum<?> name, double value) {
        teaseLib.set(namespace, name, value);
    }

    public void set(Enum<?> name, String value) {
        teaseLib.set(namespace, name, value);
    }

    public void set(String name, boolean value) {
        teaseLib.set(namespace, name, value);
    }

    public void set(String name, int value) {
        teaseLib.set(namespace, name, value);
    }

    public void set(String name, double value) {
        teaseLib.set(namespace, name, value);
    }

    public void set(String name, String value) {
        teaseLib.set(namespace, name, value);
    }

    public boolean getBoolean(String name) {
        return teaseLib.getBoolean(namespace, name);
    }

    public int getInteger(String name) {
        return teaseLib.getInteger(namespace, name);
    }

    public double getFloat(String name) {
        return teaseLib.getFloat(namespace, name);
    }

    public String getString(String name) {
        return teaseLib.getString(namespace, name);
    }

    public boolean getBoolean(Enum<?> name) {
        return teaseLib.getBoolean(namespace, name);
    }

    public int getInteger(Enum<?> name) {
        return teaseLib.getInteger(namespace, name);
    }

    public double getFloat(Enum<?> name) {
        return teaseLib.getFloat(namespace, name);
    }

    public String getString(Enum<?> name) {
        return teaseLib.getString(namespace, name);
    }

    public TeaseLib.PersistentBoolean persistentBoolean(String name) {
        return teaseLib.new PersistentBoolean(namespace, name);
    }

    public TeaseLib.PersistentInteger persistentInteger(String name) {
        return teaseLib.new PersistentInteger(namespace, name);
    }

    public TeaseLib.PersistentFloat persistentFloat(String name) {
        return teaseLib.new PersistentFloat(namespace, name);
    }

    public TeaseLib.PersistentString persistentString(String name) {
        return teaseLib.new PersistentString(namespace, name);
    }

    public <T extends Enum<T>> TeaseLib.PersistentSequence<T> persistentSequence(
            String name, T[] values) {
        return teaseLib.new PersistentSequence<T>(namespace, name, values);
    }

    public <T extends Enum<T>> State<T>.Item state(T item) {
        return teaseLib.state(item);
    }
}
