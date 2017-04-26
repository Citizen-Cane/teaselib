package teaselib.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import teaselib.util.Item;
import teaselib.util.ItemImpl;

public abstract class AbstractUserItems implements UserItems {
    class ItemMap extends HashMap<Object, List<Item>> {
        private static final long serialVersionUID = 1L;

        public ItemMap() {
        }
    }

    protected static Item[] onlyTheOriginalItem(TeaseLib teaseLib, String domain, Object item) {
        String name;
        if (item instanceof Enum<?>) {
            name = ((Enum<?>) item).name();
        } else {
            name = item.toString();
        }
        return new Item[] { new ItemImpl(teaseLib, item,
                teaseLib.new PersistentBoolean(domain, item.getClass().getName(), name)) };
    }

    Map<String, ItemMap> userItems = new HashMap<String, ItemMap>();

    @SuppressWarnings("unchecked")
    @Override
    public List<Item> get(TeaseLib teaseLib, String domain, Object item) {
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

    protected abstract Item[] createUserItems(TeaseLib teaseLib, String domain, Object item);

    protected Item item(TeaseLib teaseLib, Object item, String namespace, String name,
            String displayName, Enum<?>[] peers, Enum<?>... attributes) {
        return item(teaseLib, TeaseLib.DefaultDomain, namespace, name, displayName, item, peers,
                attributes);
    }

    protected Item item(TeaseLib teaseLib, String domain, String namespace, String name,
            String displayName, Object item, Enum<?>[] peers, Enum<?>... attributes) {
        return new ItemImpl(teaseLib, item, teaseLib.new PersistentBoolean(domain, namespace, name),
                displayName, peers, attributes);
    }
}
