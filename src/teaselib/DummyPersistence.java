package teaselib;

import teaselib.persistence.Clothing;
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
    public boolean getBoolean(String name) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void set(String name, boolean value) {
        // TODO Auto-generated method stub

    }

    @Override
    public Item get(Toys toy) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Item get(Clothing item) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Item getToy(String item) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Item getClothingItem(String item) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getVariable(TextVariable name) {
        return null;
    }
}
