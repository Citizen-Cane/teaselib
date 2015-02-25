package teaselib;

import teaselib.persistence.Clothes;
import teaselib.persistence.Item;
import teaselib.persistence.Toys;

public interface Persistence {

    String get(String name);

    void set(String name, String value);

    /**
     * Return the corresponding persistent toy object
     * 
     * @param toy
     * @return
     */
    Item get(Toys toy);

    /**
     * Return the corresponding persistent clothes item
     * 
     * @param item
     * @return
     */
    Item get(Clothes item);

}
