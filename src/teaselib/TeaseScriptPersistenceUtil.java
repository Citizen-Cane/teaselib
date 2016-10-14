package teaselib;

import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;
import teaselib.core.TeaseScriptBase;
import teaselib.util.Item;
import teaselib.util.Items;

/**
 * @author Citizen-Cane
 *
 *         Implements non-object oriented persistence helpers.
 */

@SuppressWarnings("deprecation")

public class TeaseScriptPersistenceUtil extends TeaseScriptPersistence {
    protected TeaseScriptPersistenceUtil(TeaseLib teaseLib,
            ResourceLoader resources, Actor actor, String namespace) {
        super(teaseLib, resources, actor, namespace);
    }

    protected TeaseScriptPersistenceUtil(TeaseScriptBase script, Actor actor) {
        super(script, actor);
    }

    public void clear(String name) {
        teaseLib.clear(TeaseLib.DefaultDomain, namespace, name);
    }

    public void clear(Enum<?> name) {
        teaseLib.clear(TeaseLib.DefaultDomain, name);
    }

    public void set(Enum<?> name, boolean value) {
        teaseLib.set(TeaseLib.DefaultDomain, name, value);
    }

    public void set(Enum<?> name, int value) {
        teaseLib.set(TeaseLib.DefaultDomain, name, value);
    }

    public void set(Enum<?> name, long value) {
        teaseLib.set(TeaseLib.DefaultDomain, name, value);
    }

    public void set(Enum<?> name, double value) {
        teaseLib.set(TeaseLib.DefaultDomain, name, value);
    }

    public void set(Enum<?> name, String value) {
        teaseLib.set(TeaseLib.DefaultDomain, name, value);
    }

    public void set(String name, boolean value) {
        teaseLib.set(TeaseLib.DefaultDomain, namespace, name, value);
    }

    public void set(String name, int value) {
        teaseLib.set(TeaseLib.DefaultDomain, namespace, name, value);
    }

    public void set(String name, long value) {
        teaseLib.set(TeaseLib.DefaultDomain, namespace, name, value);
    }

    public void set(String name, double value) {
        teaseLib.set(TeaseLib.DefaultDomain, namespace, name, value);
    }

    public void set(String name, String value) {
        teaseLib.set(TeaseLib.DefaultDomain, namespace, name, value);
    }

    public boolean getBoolean(String name) {
        return teaseLib.getBoolean(TeaseLib.DefaultDomain, namespace, name);
    }

    public int getInteger(String name) {
        return teaseLib.getInteger(TeaseLib.DefaultDomain, namespace, name);
    }

    public long getLong(String name) {
        return teaseLib.getInteger(TeaseLib.DefaultDomain, namespace, name);
    }

    public double getFloat(String name) {
        return teaseLib.getFloat(TeaseLib.DefaultDomain, namespace, name);
    }

    public String getString(String name) {
        return teaseLib.getString(TeaseLib.DefaultDomain, namespace, name);
    }

    public boolean getBoolean(Enum<?> name) {
        return teaseLib.getBoolean(TeaseLib.DefaultDomain, name);
    }

    public int getInteger(Enum<?> name) {
        return teaseLib.getInteger(TeaseLib.DefaultDomain, name);
    }

    public long getLong(Enum<?> name) {
        return teaseLib.getLong(TeaseLib.DefaultDomain, name);
    }

    public double getFloat(Enum<?> name) {
        return teaseLib.getFloat(TeaseLib.DefaultDomain, name);
    }

    public String getString(Enum<?> name) {
        return teaseLib.getString(TeaseLib.DefaultDomain, name);
    }

    // public void clear(String global, String name) {
    // teaseLib.clear(global, name);
    // }
    //
    // public void clear(String global, Enum<?> name) {
    // teaseLib.clear(global, name);
    // }
    //
    // public void set(String global, Enum<?> name, boolean value) {
    // teaseLib.set(global, name, value);
    // }
    //
    // public void set(String global, Enum<?> name, int value) {
    // teaseLib.set(global, name, value);
    // }
    //
    // public void set(String global, Enum<?> name, long value) {
    // teaseLib.set(global, name, value);
    // }
    //
    // public void set(String global, Enum<?> name, double value) {
    // teaseLib.set(global, name, value);
    // }
    //
    // public void set(String global, Enum<?> name, String value) {
    // teaseLib.set(global, name, value);
    // }
    //
    // public void set(String global, String name, boolean value) {
    // teaseLib.set(global, name, value);
    // }
    //
    // public void set(String global, String name, int value) {
    // teaseLib.set(global, name, value);
    // }
    //
    // public void set(String global, String name, long value) {
    // teaseLib.set(global, name, value);
    // }
    //
    // public void set(String global, String name, double value) {
    // teaseLib.set(global, name, value);
    // }
    //
    // public void set(String global, String name, String value) {
    // teaseLib.set(global, name, value);
    // }
    //
    // public boolean getBoolean(String global, String name) {
    // return teaseLib.getBoolean(global, name);
    // }
    //
    // public int getInteger(String global, String name) {
    // return teaseLib.getInteger(global, name);
    // }
    //
    // public long getLong(String global, String name) {
    // return teaseLib.getInteger(global, name);
    // }
    //
    // public double getFloat(String global, String name) {
    // return teaseLib.getFloat(global, name);
    // }
    //
    // public String getString(String global, String name) {
    // return teaseLib.getString(global, name);
    // }
    //
    // public boolean getBoolean(String global, Enum<?> name) {
    // return teaseLib.getBoolean(global, name);
    // }
    //
    // public int getInteger(String global, Enum<?> name) {
    // return teaseLib.getInteger(global, name);
    // }
    //
    // public long getLong(String global, Enum<?> name) {
    // return teaseLib.getLong(global, name);
    // }
    //
    // public double getFloat(String global, Enum<?> name) {
    // return teaseLib.getFloat(global, name);
    // }
    //
    // public String getString(String global, Enum<?> name) {
    // return teaseLib.getString(global, name);
    // }

    @Deprecated
    public Item<Toys> toy(Toys item) {
        return teaseLib.getToy(TeaseLib.DefaultDomain, item);
    }

    @Deprecated
    public Item<String> toy(String item) {
        return teaseLib.getToy(TeaseLib.DefaultDomain, item);
    }

    @Deprecated
    public Item<Clothes> clothing(Object wearer, Clothes item) {
        return teaseLib.getClothing(wearer, item);
    }

    @Deprecated
    public Item<String> clothing(Object wearer, String item) {
        return teaseLib.getClothing(wearer, item);
    }

    @Deprecated
    public <T extends Enum<?>> Items<T> toys(T... toys) {
        Items<T> items = new Items<T>();
        for (T toy : toys) {
            Item<T> item = teaseLib.getToy(TeaseLib.DefaultDomain, toy);
            items.add(item);
        }
        return items;
    }

    @Deprecated
    public <T extends Enum<?>> Items<T> toys(T[]... toys) {
        Items<T> items = new Items<T>();
        for (T[] selection : toys) {
            items.addAll(toys(selection));
        }
        return items;
    }

    @Deprecated
    public Items<String> toys(String... toys) {
        Items<String> items = new Items<String>();
        for (String toy : toys) {
            Item<String> item = teaseLib.getToy(TeaseLib.DefaultDomain, toy);
            items.add(item);
        }
        return items;
    }

    @Deprecated
    public Items<String> toys(String[]... toys) {
        Items<String> items = new Items<String>();
        for (String[] selection : toys) {
            items.addAll(toys(selection));
        }
        return items;
    }

    @Deprecated
    public <C extends Enum<C>> Items<C> clothes(Object wearer, C... clothes) {
        Items<C> items = new Items<C>();
        for (C clothing : clothes) {
            Item<C> item = teaseLib.getClothing(wearer, clothing);
            items.add(item);
        }
        return items;
    }

    @Deprecated
    public <C extends Enum<C>> Items<C> clothes(Object wearer,
            C[]... clothing) {
        Items<C> items = new Items<C>();
        for (C[] selection : clothing) {
            items.addAll(clothes(wearer, selection));
        }
        return items;
    }

    @Deprecated
    public Items<String> clothes(Object wearer, String... clothes) {
        Items<String> items = new Items<String>();
        for (String clothing : clothes) {
            Item<String> item = teaseLib.getClothing(wearer, clothing);
            items.add(item);
        }
        return items;
    }

    @Deprecated
    public Items<String> clothes(Object wearer, String[]... clothing) {
        Items<String> items = new Items<String>();
        for (String[] selection : clothing) {
            items.addAll(clothes(wearer, selection));
        }
        return items;
    }

}
