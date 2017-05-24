package teaselib.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import teaselib.Body;
import teaselib.Clothes;
import teaselib.Household;
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

    protected Item[] onlyTheOriginalItem(TeaseLib teaseLib, String domain, QualifiedItem<?> item) {
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

    protected abstract Item[] createUserItems(TeaseLib teaseLib, String domain, QualifiedItem<?> item);

    protected Item item(TeaseLib teaseLib, QualifiedItem<?> item, String namespace, String name, String displayName,
            Enum<?>... attributes) {
        return item(teaseLib, TeaseLib.DefaultDomain, namespace, name, displayName, item, defaults(item), attributes);
    }

    protected Item item(TeaseLib teaseLib, QualifiedItem<?> item, String namespace, String name, String displayName,
            Enum<?>[] peers, Enum<?>... attributes) {
        return item(teaseLib, TeaseLib.DefaultDomain, namespace, name, displayName, item, peers, attributes);
    }

    protected Item item(TeaseLib teaseLib, String domain, String namespace, String name, String displayName,
            QualifiedItem<?> item, Enum<?>[] peers, Enum<?>... attributes) {
        return new ItemImpl(teaseLib, domain, item.value, teaseLib.new PersistentBoolean(domain, namespace, name),
                displayName, peers, attributes);
    }

    public Enum<?>[] array(Enum<?>[] defaults, Enum<?>... additional) {
        Enum<?>[] extended = new Enum<?>[defaults.length + additional.length];
        System.arraycopy(defaults, 0, extended, 0, defaults.length);
        System.arraycopy(additional, 0, extended, defaults.length, additional.length);
        return extended;
    }

    @Override
    public Enum<?>[] defaults(QualifiedItem<?> item) {
        if (item.namespace().equalsIgnoreCase(Toys.class.getName())) {
            return getToyDefaults(item);
        } else if (item.namespace().equalsIgnoreCase(Clothes.class.getName())) {
            return getClothesDefaults(item);
        } else if (item.namespace().equalsIgnoreCase(Household.class.getName())) {
            return getHouseholdDefaults(item);
        } else {
            throw new IllegalArgumentException("Defaults not defined for " + item);
        }
    }

    private static Enum<?>[] getClothesDefaults(@SuppressWarnings("unused") QualifiedItem<?> item) {
        return new Body[] {};
    }

    private static Enum<?>[] getHouseholdDefaults(@SuppressWarnings("unused") QualifiedItem<?> item) {
        return new Body[] {};
    }

    private static Enum<?>[] getToyDefaults(QualifiedItem<?> item) {
        if (item.equals(Toys.Buttplug)) {
            return new Body[] { Body.InButt };
        } else if (item.equals(Toys.Ankle_Restraints)) {
            return new Body[] { Body.AnklesTied };
        } else if (item.equals(Toys.Wrist_Restraints)) {
            return new Body[] { Body.WristsTied };
        } else if (item.equals(Toys.Gag)) {
            return new Body[] { Body.InMouth };
        } else if (item.equals(Toys.Spanking_Implement)) {
            return new Body[] {};
        } else if (item.equals(Toys.Collar)) {
            return new Body[] { Body.AroundNeck };
        } else if (item.equals(Toys.Nipple_Clamps)) {
            return new Body[] { Body.OnNipples };
        } else if (item.equals(Toys.Chastity_Device)) {
            return new Body[] { Body.OnPenis, Body.CantJerkOff };
        } else if (item.equals(Toys.Dildo)) {
            return new Body[] {};
        } else if (item.equals(Toys.VaginalInsert)) {
            return new Body[] { Body.InVagina };
        } else if (item.equals(Toys.Vibrator)) {
            return new Body[] {};
        } else if (item.equals(Toys.Ball_Stretcher)) {
            return new Body[] { Body.OnBalls };
        } else if (item.equals(Toys.Blindfold)) {
            return new Body[] { Body.CantSee };
        } else if (item.equals(Toys.Cockring)) {
            return new Body[] { Body.AroundCockBase };
        } else if (item.equals(Toys.Enema_Bulb)) {
            return new Body[] { Body.InButt };
        } else if (item.equals(Toys.Enema_Kit)) {
            return new Body[] { Body.InButt };
        } else if (item.equals(Toys.Humbler)) {
            return new Body[] { Body.OnBalls, Body.CantStand };
        } else if (item.equals(Toys.Pussy_Clamps)) {
            return new Body[] { Body.OnLabia };
        } else if (item.equals(Toys.Spreader_Bar)) {
            return new Body[] {};
        } else if (item.equals(Toys.EStim_Device)) {
            return new Body[] {};
        } else if (item.equals(Toys.Chains)) {
            return new Body[] {};
        } else if (item.equals(Toys.Rope)) {
            return new Body[] {};
        } else if (item.equals(Toys.Doll)) {
            return new Body[] {};
        } else if (item.equals(Toys.Husband)) {
            return new Body[] {};
        } else if (item.equals(Toys.Wife)) {
            return new Body[] {};
        } else if (item.equals(Toys.Strap_On)) {
            return new Body[] {};
        } else {
            throw new IllegalArgumentException("Defaults not defined for " + item);
        }
    }
}
