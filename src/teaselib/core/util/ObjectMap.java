package teaselib.core.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ObjectMap {
    private final Map<Object, Supplier<?>> suppliers = new HashMap<>();
    private final Map<Object, Object> realized = new HashMap<>();

    private final boolean writeOnce = true;

    public <T extends Supplier<?>> ObjectMap store(Object key, T supplier) {
        dontOverwrite(key);
        return storeInternal(key, supplier);
    }

    private <T extends Supplier<?>> ObjectMap storeInternal(Object key, T supplier) {
        suppliers.put(key, supplier);
        return this;
    }

    public <T> T store(T value) {
        Class<? extends Object> key = value.getClass();
        dontOverwrite(key);
        return storeInternal(value, key);
    }

    private <T> T storeInternal(T value, Class<? extends Object> key) {
        realized.put(key, value);
        return value;
    }

    public <T> T store(Object key, T value) {
        dontOverwrite(key);
        return storeInternal(key, value);
    }

    private <T> T storeInternal(Object key, T value) {
        realized.put(key, value);
        return value;
    }

    private void dontOverwrite(Object key) {
        if (writeOnce && (suppliers.containsKey(key) || realized.containsKey(key))) {
            throw new IllegalArgumentException(key + " is read-only");
        }
    }

    public <T> T getOrDefault(Class<T> clazz, Supplier<T> supplier) {
        T t = get(clazz);
        return t != null ? t : store(clazz, supplier).get(clazz);
    }

    public <T> T get(Class<T> clazz) {
        return get((Object) clazz);
    }

    public <T> T getOrDefault(Object key, Supplier<T> supplier) {
        T t = get(key);
        return t != null ? t : store(key, supplier).get(key);
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
                storeInternal(key, value);
                return value;
            } else {
                return null;
            }
        }
    }
}
