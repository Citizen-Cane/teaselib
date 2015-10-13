package teaselib.hosts;

import java.util.HashMap;
import java.util.Map;

import teaselib.core.Persistence;

public class DummyPersistence implements Persistence {

    public final static String True = "true";
    public final static String False = "false";

    public final Map<String, String> storage = new HashMap<String, String>();

    @Override
    public boolean has(String name) {
        return storage.containsKey(name);
    }

    @Override
    public String get(String name) {
        return storage.get(name);
    }

    @Override
    public void set(String name, String value) {
        storage.put(name, value);
    }

    @Override
    public boolean getBoolean(String name) {
        String value = get(name);
        if (value == null) {
            return false;
        } else {
            return value.equals(True);
        }
    }

    @Override
    public void set(String name, boolean value) {
        set(name, value ? True : False);
    }

    @Override
    public void clear(String name) {
        storage.remove(name);
    }

    @Override
    public String get(TextVariable name, String locale) {
        throw new UnsupportedOperationException(name.name());
    }

    @Override
    public void set(TextVariable name, String locale, String value) {
        throw new UnsupportedOperationException(name.name());
    }

}
