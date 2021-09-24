package teaselib.util;

import java.util.Arrays;
import java.util.List;

import teaselib.core.util.Persist;
import teaselib.core.util.Persist.Persistable;
import teaselib.core.util.QualifiedItem;
import teaselib.core.util.QualifiedString;
import teaselib.core.util.Storage;

/**
 * @author citizen-cane
 *
 */
public class ItemGuid implements Persistable {
    private static final String HEADER = "guid:";

    private final QualifiedItem item;

    public static ItemGuid fromGuid(String item) {
        return new ItemGuid(item);
    }

    public static ItemGuid from(QualifiedItem item) {
        return new ItemGuid(item);
    }

    public static ItemGuid from(QualifiedItem kind, String name) {
        return new ItemGuid(new QualifiedString(kind.namespace(), kind.name(), name));
    }

    public static ItemGuid from(String namespace, String kind, String name) {
        return new ItemGuid(new QualifiedString(namespace, kind, name));
    }

    private ItemGuid(String item) {
        if (isGuid(item)) {
            var qualifiedItem = item.substring(HEADER.length());
            this.item = new QualifiedString(qualifiedItem);
        } else {
            this.item = new QualifiedString(item);
        }
        if (!this.item.guid().isPresent()) {
            throw new IllegalArgumentException("Qualified item without guid: " + item);
        }
    }

    private ItemGuid(QualifiedItem item) {
        if (item.guid().isPresent()) {
            this.item = item;
        } else {
            throw new IllegalArgumentException("Qualified item without guid: " + item);
        }
    }

    public ItemGuid(Storage storage) throws ReflectiveOperationException {
        this(new QualifiedString(storage.next()));
    }

    public QualifiedItem item() {
        return item;
    }

    public QualifiedItem kind() {
        return new QualifiedString(item.namespace(), item.name());
    }

    public String name() {
        return item.guid().orElseThrow();
    }

    public static boolean isGuid(String name) {
        return name.startsWith(HEADER);
    }

    public static boolean isGuid(Object object) {
        return object instanceof ItemGuid || isGuid(object.toString());
    }

    @Override
    public List<String> persisted() {
        return Arrays.asList(Persist.persist(item.toString()));
    }

    @Override
    public String toString() {
        return HEADER + item;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((item == null) ? 0 : item.hashCode());
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
        if (item == null) {
            if (other.item != null)
                return false;
        } else if (!item.equals(other.item))
            return false;
        return true;
    }

}