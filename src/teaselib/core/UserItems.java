package teaselib.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import teaselib.util.Item;

public interface UserItems {
    Map<Object, List<Item>> userItems = new HashMap<Object, List<Item>>();

    List<Item> get(TeaseLib teaseLib, String domain, Object item);

    Enum<?>[] defaults(Object item);
}
