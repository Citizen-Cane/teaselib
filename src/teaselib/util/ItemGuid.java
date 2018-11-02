package teaselib.util;

import java.util.Arrays;
import java.util.List;

import teaselib.core.util.Persist;
import teaselib.core.util.Persist.Persistable;
import teaselib.core.util.Storage;

/**
 * @author citizen-cane
 *
 */
public class ItemGuid implements Persistable {
    private static final String HEADER = "guid:";

    private final String name;

    public static ItemGuid fromGuid(String guid) {
        if (isGuid(guid)) {
            return new ItemGuid(guid.substring(HEADER.length()));
        } else {
            throw new IllegalArgumentException("not a formatted guid string:" + guid);
        }
    }

    public ItemGuid(String name) {
        if (isGuid(name)) {
            throw new IllegalArgumentException("Already a formatted guid:" + name);
        } else {
            this.name = name;
        }
    }

    public ItemGuid(Storage storage) {
        String name = storage.next();
        if (isGuid(name)) {
            throw new IllegalArgumentException("Already a formatted guid:" + name);
        } else {
            this.name = name;
        }
    }

    public String name() {
        return name;
    }

    public static boolean isGuid(String name) {
        return name.startsWith(HEADER);
    }

    public static boolean isItemGuid(Object object) {
        return object instanceof ItemGuid;
    }

    public static boolean isntItemGuid(Object object) {
        return !(object instanceof ItemGuid);
    }

    @Override
    public List<String> persisted() {
        return Arrays.asList(Persist.persist(name));
    }

    @Override
    public String toString() {
        return HEADER + name;
    }
}
