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

    public Item(String name, Persistence persistence) {
        super(name, persistence);
    }

    public boolean isAvailable() {
        return get().equals("0");
    }

    public void setAvailable(boolean isAvailable) {
        set(isAvailable ? "1" : "0");
    }
}
