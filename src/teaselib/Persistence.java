package teaselib;

import teaselib.persistence.Clothing;
import teaselib.persistence.Item;
import teaselib.persistence.Toys;

public interface Persistence {

    String get(String name);

    void set(String name, String value);

    boolean getBoolean(String name);

    void set(String name, boolean value);

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
    Item get(Clothing item);

    Item getToy(String item);

    Item getClothingItem(String item);

}
