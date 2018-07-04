package teaselib.core;

import java.io.File;
import java.io.IOException;
import java.util.List;

import teaselib.core.util.QualifiedItem;
import teaselib.util.Item;

public interface UserItems {
    List<Item> get(String domain, QualifiedItem item);

    void loadItems(File path) throws IOException;

    Enum<?>[] defaults(QualifiedItem item);
}
