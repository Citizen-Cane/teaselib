/**
 * 
 */
package teaselib;

import java.util.ArrayList;
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
        return teaseLib.persistence.get(item);
    }

    public Item get(Clothing item) {
        return teaseLib.persistence.get(item);
    }

    public boolean isAvailable(Toys toy) {
        Item item = teaseLib.persistence.get(toy);
        return item.isAvailable();
    }

    public boolean isAnyAvailable(Toys... toys) {
        for (Toys toy : toys) {
            Item item = teaseLib.persistence.get(toy);
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
            Item item = teaseLib.persistence.get(toy);
            items.add(item);
        }
        return items;
    }

    public List<Item> get(Toys[]... toys) {
        List<Item> items = new ArrayList<Item>();
        for (Toys[] selection : toys) {
            for (Toys toy : selection) {
                Item item = teaseLib.persistence.get(toy);
                items.add(item);
            }
        }
        return items;
    }

    /**
     * Get values for any enumeration. This is different from toys and clothing
     * in that those are usually handled by the host.
     * 
     * @param values
     * @return
     */
    public List<Item> get(Enum<? extends Enum<?>>... values) {
        List<Item> items = new ArrayList<Item>(values.length);
        for (Enum<?> v : values) {
            items.add(new Item(namespace + "." + v.getClass().getName() + "."
                    + v.name(), v.toString(), teaseLib.persistence));
        }
        return items;
    }

    public Item get(Enum<? extends Enum<?>> value) {
        return new Item(namespace + "." + value.getClass().getName() + "."
                + value.name(), value.toString(), teaseLib.persistence);
    }

    public boolean isAnyAvailable(Clothing... clothes) {
        for (Clothing clothing : clothes) {
            Item item = teaseLib.persistence.get(clothing);
            if (item.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    public List<Item> get(Clothing... clothes) {
        List<Item> items = new ArrayList<Item>();
        for (Clothing clothing : clothes) {
            Item item = teaseLib.persistence.get(clothing);
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
        teaseLib.set(makePropertyName(name), value);
    }

    public void set(String name, int value) {
        teaseLib.set(makePropertyName(name), value);
    }

    public void set(String name, double value) {
        teaseLib.set(makePropertyName(name), value);
    }

    public void set(String name, String value) {
        teaseLib.set(makePropertyName(name), value);
    }

    public boolean getBoolean(String name) {
        return teaseLib.getBoolean(makePropertyName(name));
    }

    public int getInteger(String name) {
        return teaseLib.getInteger(makePropertyName(name));
    }

    public double getFloat(String name) {
        return teaseLib.getFloat(makePropertyName(name));
    }

    public String getString(String name) {
        return teaseLib.getString(makePropertyName(name));
    }

    public TeaseLib.PersistentBoolean persistentBoolean(String name) {
        return teaseLib.new PersistentBoolean(makePropertyName(name));
    }

    public TeaseLib.PersistentInteger persistentInteger(String name) {
        return teaseLib.new PersistentInteger(makePropertyName(name));
    }

    public TeaseLib.PersistentFloat persistentFloat(String name) {
        return teaseLib.new PersistentFloat(makePropertyName(name));
    }

    public TeaseLib.PersistentString persistentString(String name) {
        return teaseLib.new PersistentString(makePropertyName(name));
    }

    public <T extends Enum<T>> TeaseLib.PersistentSequence<T> persistentSequence(
            String name, T[] values) {
        return teaseLib.new PersistentSequence<T>(makePropertyName(name),
                values);
    }

    @Override
    public String get(Persistence.TextVariable variable) {
        return super.get(variable);
    }

    public void set(TextVariable var, String value) {
        teaseLib.persistence.set(var, actor.locale, value);
    }

    public <T extends Enum<T>> State<T>.Item state(T item) {
        return teaseLib.state(item);
    }

    public <T extends Enum<T>> State<T> state(T[] values) {
        return teaseLib.state(values);
    }
}
