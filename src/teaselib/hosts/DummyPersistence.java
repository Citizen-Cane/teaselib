package teaselib.hosts;

import teaselib.Clothing;
import teaselib.Toys;
import teaselib.core.Persistence;
import teaselib.util.Item;

public class DummyPersistence implements Persistence {

    @Override
    public boolean has(String name) {
        // TODO Auto-generated method stub
        return false;
    }

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
    public void clear(String name) {
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
    public String get(TextVariable name, String locale) {
        return null;
    }

    @Override
    public void set(TextVariable name, String locale, String value) {
        // TODO Auto-generated method stub
    }

}
