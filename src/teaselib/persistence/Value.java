/**
 * 
 */
package teaselib.persistence;

import teaselib.hosts.Persistence;

/**
 * @author someone
 *
 */
public class Value {
    protected final String name;
    public final String displayName;
    protected final Persistence persistence;

    public Value(String name, String displayName, Persistence persistence) {
        this.name = name;
        this.displayName = displayName;
        this.persistence = persistence;
    }

    public static String createDisplayName(String name) {
        String displayName = name.replace("_", " ");
        return displayName;
    }

    @Override
    public String toString() {
        return name;
    }
}
