package teaselib.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import teaselib.util.Item;
import teaselib.util.ItemImpl;

public interface UserItems {
    Map<Object, List<Item>> userItems = new HashMap<Object, List<Item>>();

    <T extends Enum<?>> List<ItemImpl> get(TeaseLib teaseLib, String domain, Object item);
}
