package teaselib.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import teaselib.Body;
import teaselib.Clothes;
import teaselib.Household;
import teaselib.Posture;
import teaselib.Toys;
import teaselib.core.util.QualifiedItem;
import teaselib.util.Item;
import teaselib.util.ItemImpl;

public abstract class AbstractUserItems implements UserItems {
    final protected TeaseLib teaseLib;
    final Map<String, ItemMap> userItems = new HashMap<>();

    class ItemMap extends HashMap<Object, List<Item>> {
        private static final long serialVersionUID = 1L;

        public ItemMap() {
        }
    }

    public AbstractUserItems(TeaseLib teaseLib) {
        super();
        this.teaseLib = teaseLib;
    }

    protected Item[] getDefaultItem(String domain, QualifiedItem<?> item) {
        return new Item[] { new ItemImpl(teaseLib, item, domain, item.name(), ItemImpl.createDisplayName(item)) };
    }

    @Override
    public List<Item> get(String domain, QualifiedItem<?> item) {
        ItemMap itemMap = userItems.get(domain);
        if (itemMap == null) {
            itemMap = new ItemMap();
            userItems.put(domain, itemMap);
        }

        if (!itemMap.containsKey(item)) {
            List<Item> items = Arrays.asList(createUserItems(domain, item));
            itemMap.put(item, items);
            return items;
        } else {
            return itemMap.get(item);
        }
    }

    protected abstract Item[] createUserItems(String domain, QualifiedItem<?> item);

    protected Item item(QualifiedItem<?> item, String name, String displayName, Enum<?>... attributes) {
        return item(TeaseLib.DefaultDomain, name, displayName, item, defaults(item), attributes);
    }

    protected Item item(QualifiedItem<?> item, String name, String displayName, Enum<?>[] defaultPeers,
            Enum<?>... attributes) {
        return item(TeaseLib.DefaultDomain, name, displayName, item, defaultPeers, attributes);
    }

    protected Item item(String domain, String name, String displayName, QualifiedItem<?> item, Enum<?>[] defaultPeers,
            Enum<?>... attributes) {
        return new ItemImpl(teaseLib, item.value, domain, name, displayName, defaultPeers, attributes);
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

    private static Enum<?>[] getClothesDefaults(QualifiedItem<?> item) {
        return new Body[] {};
    }

    private static Enum<?>[] getHouseholdDefaults(QualifiedItem<?> item) {
        return new Body[] {};
    }

    /**
     * Get system default peers for items. The defaults depend on the meaning of the item. Item have defaults if they
     * have a clear application or are applied to a distinct spot, like putting a collar around one's neck. Same for
     * ankle restraints.
     * <p>
     * On the other hand wrist restraints don't have a default, since wrists can be tied before and behind the body.
     * 
     * @param item
     *            The item to get defaults for.
     * @return The defaults for the item. An item may not have defaults, in this case the returned array is empty.
     */
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
            return new Body[] { Body.Blindfolded };
        } else if (item.equals(Toys.Cockring)) {
            return new Body[] { Body.AroundCockBase };
        } else if (item.equals(Toys.Enema_Bulb)) {
            return new Body[] { Body.InButt };
        } else if (item.equals(Toys.Enema_Kit)) {
            return new Body[] { Body.InButt };
        } else if (item.equals(Toys.Humbler)) {
            return new Enum<?>[] { Body.OnBalls, Posture.CantStand, Posture.CantSitOnChair };
        } else if (item.equals(Toys.Pussy_Clamps)) {
            return new Body[] { Body.OnLabia, Body.OnBalls };
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
