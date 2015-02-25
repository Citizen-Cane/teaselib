/**
 * 
 */
package teaselib.persistence;

import teaselib.Persistence;

/**
 * @author someone
 *
 */
public class Value {
    private final String name;
    private final Persistence persistence;

    public Value(String name, Persistence persistence) {
        this.name = name;
        this.persistence = persistence;
    }

    public String get() {
        return persistence.get(name);
    }

    public void set(String value) {
        persistence.set(name, value);
    }
}
