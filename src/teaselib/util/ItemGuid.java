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

    public static ItemGuid fromGuid(Object guid) {
        if (guid instanceof ItemGuid) {
            return (ItemGuid) guid;
        } else {
            return fromGuid(guid.toString());
        }
    }

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

    public ItemGuid(Storage storage) throws ReflectiveOperationException {
        this.name = storage.next();
        if (isGuid(name)) {
            throw new IllegalArgumentException("Already a formatted guid:" + name);
        }
    }

    public String name() {
        return name;
    }

    public static boolean isGuid(String name) {
        return name.startsWith(HEADER);
    }

    public static boolean isGuid(Object object) {
        return object instanceof ItemGuid || isGuid(object.toString());
    }

    @Override
    public List<String> persisted() {
        return Arrays.asList(Persist.persist(name));
    }

    @Override
    public String toString() {
        return HEADER + name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ItemGuid other = (ItemGuid) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

}
