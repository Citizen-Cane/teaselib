package teaselib;

import teaselib.persistence.Clothes;
import teaselib.persistence.Item;
import teaselib.persistence.Toys;

public class DummyPersistence implements Persistence {

    @Override
    public String get(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void set(String name, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public Item get(Toys toy) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Item get(Clothes item) {
        // TODO Auto-generated method stub
        return null;
    }

}
