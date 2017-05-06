package teaselib.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import teaselib.Body;
import teaselib.Toys;
import teaselib.core.util.QualifiedItem;
import teaselib.util.Item;
import teaselib.util.ItemImpl;

public abstract class AbstractUserItems implements UserItems {
    class ItemMap extends HashMap<Object, List<Item>> {
        private static final long serialVersionUID = 1L;

        public ItemMap() {
        }
    }

    protected static Item[] onlyTheOriginalItem(TeaseLib teaseLib, String domain,
            QualifiedItem<?> item) {
        return new Item[] { new ItemImpl(teaseLib, domain, item,
                teaseLib.new PersistentBoolean(domain, item.namespace(), item.name())) };
    }

    Map<String, ItemMap> userItems = new HashMap<String, ItemMap>();

    @SuppressWarnings("unchecked")
    @Override
    public List<Item> get(TeaseLib teaseLib, String domain, QualifiedItem<?> item) {
        ItemMap itemMap = userItems.get(domain);
        if (itemMap == null) {
            itemMap = new ItemMap();
            userItems.put(domain, itemMap);
        }

        if (!itemMap.containsKey(item)) {
            List<Item> items = Arrays.asList(createUserItems(teaseLib, domain, item));
            itemMap.put(item, items);
            return items;
        } else {
            @SuppressWarnings("rawtypes")
            List items = itemMap.get(item);
            return items;
        }
    }

    protected abstract Item[] createUserItems(TeaseLib teaseLib, String domain,
            QualifiedItem<?> item);

    protected Item item(TeaseLib teaseLib, QualifiedItem<?> item, String namespace, String name,
            String displayName, Enum<?>... attributes) {
        return item(teaseLib, TeaseLib.DefaultDomain, namespace, name, displayName, item,
                defaults(item), attributes);
    }

    protected Item item(TeaseLib teaseLib, QualifiedItem<?> item, String namespace, String name,
            String displayName, Enum<?>[] peers, Enum<?>... attributes) {
        return item(teaseLib, TeaseLib.DefaultDomain, namespace, name, displayName, item, peers,
                attributes);
    }

    protected Item item(TeaseLib teaseLib, String domain, String namespace, String name,
            String displayName, QualifiedItem<?> item, Enum<?>[] peers, Enum<?>... attributes) {
        return new ItemImpl(teaseLib, domain, item.value,
                teaseLib.new PersistentBoolean(domain, namespace, name), displayName, peers,
                attributes);
    }

    public <T> T[] array(T[] defaults, T... additional) {
        @SuppressWarnings("unchecked")
        T[] extended = (T[]) new Object[defaults.length + additional.length];
        System.arraycopy(defaults, 0, extended, 0, defaults.length);
        System.arraycopy(additional, 0, extended, defaults.length, additional.length);
        return extended;
    }

    @Override
    public Enum<?>[] defaults(QualifiedItem<?> item) {
        if (item.equals(Toys.Buttplug)) {
            return new Body[] { Body.SomethingInButt };
        } else if (item.equals(Toys.Ankle_Restraints)) {
            return new Body[] { Body.AnklesTied };
        } else if (item.equals(Toys.Wrist_Restraints)) {
            return new Body[] { Body.WristsTied };
        } else if (item.equals(Toys.Gag)) {
            return new Body[] { Body.SomethingInMouth };
        } else if (item.equals(Toys.Spanking_Implement)) {
            return new Body[] {};
        } else if (item.equals(Toys.Collar)) {
            return new Body[] { Body.AroundNeck };
        } else if (item.equals(Toys.Chastity_Device)) {
            return new Body[] { Body.SomethingOnPenis, Body.CannotJerkOff };
        } else if (item.equals(Toys.Dildo)) {
            return new Body[] {};
        } else if (item.equals(Toys.VaginalInsert)) {
            return new Body[] { Body.SomethingInVagina };
        } else if (item.equals(Toys.Vibrator)) {
            return new Body[] { Body.SomethingOnClit }; // TODO for men too
        } else if (item.equals(Toys.EStim_Device)) {
            return new Body[] {};
        } else {
            throw new IllegalArgumentException("Defaults not defined for " + item);
        }
    }
}
