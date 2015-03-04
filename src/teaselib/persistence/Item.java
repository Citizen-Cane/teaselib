/**
 * 
 */
package teaselib.persistence;

import teaselib.Persistence;

/**
 * @author someone
 *
 */
public class Item extends Value {

    public Item(String name, String displayName, Persistence persistence) {
        super(name, displayName, persistence);
    }

    public boolean isAvailable() {
        return persistence.getBoolean(name);
    }

    public void setAvailable(boolean isAvailable) {
        persistence.set(name, isAvailable);
    }
}
