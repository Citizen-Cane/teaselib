package teaselib.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import teaselib.core.util.QualifiedItem;
import teaselib.util.Item;

public interface UserItems {
    Map<Object, List<Item>> userItems = new HashMap<Object, List<Item>>();

    List<Item> get(String domain, QualifiedItem<?> item);

    Enum<?>[] defaults(QualifiedItem<?> item);
}
