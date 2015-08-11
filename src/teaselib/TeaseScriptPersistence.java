/**
 * 
 */
package teaselib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import teaselib.core.Persistence;
import teaselib.core.Persistence.TextVariable;
import teaselib.core.ResourceLoader;
import teaselib.core.TeaseScriptBase;
import teaselib.util.Item;

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

    public Item get(Toys item) {
        return teaseLib.get(item);
    }

    public Item get(Clothing item) {
        return teaseLib.get(item);
    }

    public boolean isAvailable(Toys toy) {
        Item item = teaseLib.get(toy);
        return item.isAvailable();
    }

    public boolean isAnyAvailable(Toys... toys) {
        for (Toys toy : toys) {
            Item item = teaseLib.get(toy);
            if (item.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    public Toys[] getAvailable(Toys... toys) {
        List<Toys> available = new ArrayList<Toys>();
        for (Toys toy : toys) {
            if (isAnyAvailable(toy)) {
                available.add(toy);
            }
        }
        Toys[] t = new Toys[available.size()];
        return available.toArray(t);
    }

    public Clothing[] getAvailable(Clothing... clothes) {
        List<Clothing> available = new ArrayList<Clothing>();
        for (Clothing clothing : clothes) {
            if (isAnyAvailable(clothing)) {
                available.add(clothing);
            }
        }
        Clothing[] c = new Clothing[available.size()];
        return available.toArray(c);
    }

    public List<Item> get(Toys... toys) {
        List<Item> items = new ArrayList<Item>();
        for (Toys toy : toys) {
            Item item = teaseLib.get(toy);
            items.add(item);
        }
        return items;
    }

    public List<Item> get(Toys[]... toys) {
        List<Item> items = new ArrayList<Item>();
        for (Toys[] selection : toys) {
            for (Toys toy : selection) {
                Item item = teaseLib.get(toy);
                items.add(item);
            }
        }
        return items;
    }

    /**
     * Get items from a enumeration. This is different from toys and clothing in
     * that toys and clothing is usually handled by the host, whereas
     * script-related enumerations are handled by the script.
     * 
     * @param values
     * @return A list of items whose names are based on the enumeration members
     */
    public List<Item> get(Enum<? extends Enum<?>>... values) {
        if (values.length > 0) {
            final String namespace = this.namespace + "."
                    + values[0].getClass().getSimpleName();
            return teaseLib.get(namespace, values);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Get the item from an enumeration member
     * 
     * @param value
     *            The enumeration value to get the item for
     * @return The item of the enumeration member
     */
    public Item get(Enum<? extends Enum<?>> value) {
        final String namespace = this.namespace + "."
                + value.getClass().getSimpleName();
        return teaseLib.get(namespace, value);
    }

    public boolean isAnyAvailable(Clothing... clothes) {
        for (Clothing clothing : clothes) {
            Item item = teaseLib.get(clothing);
            if (item.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    public List<Item> get(Clothing... clothes) {
        List<Item> items = new ArrayList<Item>();
        for (Clothing clothing : clothes) {
            Item item = teaseLib.get(clothing);
            items.add(item);
        }
        return items;
    }

    public boolean isAnyAvailable(List<Item> items) {
        for (Item item : items) {
            if (item.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    public boolean isAnyAvailable(Item... items) {
        for (Item item : items) {
            if (item.isAvailable()) {
                return true;
            }
        }
        return false;
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

    public String get(Persistence.TextVariable variable) {
        return teaseLib.get(variable, actor.locale);
    }

    public void set(TextVariable var, String value) {
        teaseLib.set(var, actor.locale, value);
    }

    public <T extends Enum<T>> State<T>.Item state(T item) {
        return teaseLib.state(item);
    }

    public <T extends Enum<T>> State<T> state(T[] values) {
        return teaseLib.state(values);
    }
}
