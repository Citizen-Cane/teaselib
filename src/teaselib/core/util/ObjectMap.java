package teaselib.core.util;

import java.util.HashMap;
import java.util.Map;

public class ObjectMap {
    private final Map<Object, Object> map = new HashMap<>();

    public <T> void store(Object key, T value) {
        map.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Object key) {
        return (T) map.get(key);
    }
}
