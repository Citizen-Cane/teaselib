package teaselib.core.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ObjectMap {
    private final Map<Object, Supplier<?>> suppliers = new HashMap<>();
    private final Map<Object, Object> realized = new HashMap<>();

    public <T extends Supplier<?>> ObjectMap store(Object key, T value) {
        suppliers.put(key, value);
        return this;
    }

    public <T> ObjectMap store(Object key, T value) {
        realized.put(key, value);
        return this;
    }

    public <T> T get(Object key) {
        @SuppressWarnings("unchecked")
        T value = (T) realized.get(key);
        if (value != null) {
            return value;
        } else {
            @SuppressWarnings("unchecked")
            Supplier<T> supplier = (Supplier<T>) suppliers.get(key);
            if (supplier != null) {
                value = supplier.get();
                store(key, value);
                return value;
            } else {
                return null;
            }
        }
    }
}
