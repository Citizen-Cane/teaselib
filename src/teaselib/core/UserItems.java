package teaselib.core;

import java.util.List;

import teaselib.core.util.QualifiedItem;
import teaselib.util.Item;

public interface UserItems {
    List<Item> get(String domain, QualifiedItem item);

    Enum<?>[] defaults(QualifiedItem item);
}
