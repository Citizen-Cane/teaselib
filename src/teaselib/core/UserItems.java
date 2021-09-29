package teaselib.core;

import java.net.URL;
import java.util.List;

import teaselib.core.util.QualifiedItem;
import teaselib.core.util.QualifiedString;
import teaselib.util.Item;

public interface UserItems {
    List<Item> get(String domain, QualifiedString item);

    void addItems(URL path);

    Enum<?>[] defaults(QualifiedItem item);
}
