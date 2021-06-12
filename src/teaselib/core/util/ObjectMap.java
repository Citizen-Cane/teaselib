package teaselib.core.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

import teaselib.core.Closeable;

public class ObjectMap implements Closeable {
    private final Map<Object, Supplier<?>> suppliers = new HashMap<>();
    private final Map<Object, Object> realized = new HashMap<>();

    private final boolean writeOnce = true;

    public <T extends Supplier<?>> void store(Object key, T supplier) {
        dontOverwrite(key);
        storeInternal(key, supplier);
    }

    private <T extends Supplier<?>> void storeInternal(Object key, T supplier) {
        suppliers.put(key, supplier);
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
        if (t != null) {
            return t;
        } else {
            store(clazz, supplier);
            return get(clazz);
        }
    }

    public <T> boolean has(Class<T> clazz) {
        return realized.containsKey(clazz) || suppliers.containsKey(clazz);
    }

    public <T> T get(Class<T> clazz) {
        return get((Object) clazz);
    }

    public <T> T getOrDefault(Object key, Supplier<T> supplier) {
        T t = get(key);
        if (t != null) {
            return t;
        } else {
            store(key, supplier);
            return get(key);
        }
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

    @Override
    public void close() {
        Exception first = null;
        Set<Object> closed = new HashSet<>();
        for (Entry<Object, Object> entry : realized.entrySet()) {
            if (entry.getValue() instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) entry.getValue()).close();
                } catch (Exception e) {
                    if (first == null) {
                        first = e;
                    }
                } finally {
                    closed.add(entry.getKey());
                }
            }
        }

        closed.stream().forEach(realized::remove);

        if (first != null) {
            throw ExceptionUtil.asRuntimeException(first);
        }
    }

}
