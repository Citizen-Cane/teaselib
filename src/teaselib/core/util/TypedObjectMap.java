package teaselib.core.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

import teaselib.core.Closeable;

public class TypedObjectMap implements Closeable {
    private final Map<Class<?>, Supplier<?>> suppliers = new HashMap<>();
    private final Map<Class<?>, Object> realized = new HashMap<>();

    private static final boolean writeOnce = true;

    public <T> void store(Class<? extends T> key, Supplier<T> supplier) {
        dontOverwrite(key);
        storeInternal(key, supplier);
    }

    private <T> void storeInternal(Class<? extends T> key, Supplier<T> supplier) {
        suppliers.put(key, supplier);
    }

    public <T> T store(T value) {
        @SuppressWarnings("unchecked")
        Class<? extends T> key = (Class<? extends T>) value.getClass();
        dontOverwrite(key);
        return storeInternal(key, value);
    }

    public <T> T store(Class<? extends T> key, T value) {
        dontOverwrite(key);
        return storeInternal(key, value);
    }

    private <T> T storeInternal(Class<? extends T> key, T value) {
        realized.put(key, value);
        return value;
    }

    private void dontOverwrite(Class<?> key) {
        if (writeOnce && (suppliers.containsKey(key) || realized.containsKey(key))) {
            throw new IllegalArgumentException(key + " is read-only");
        }
    }

    public <T> T getOrDefault(Class<T> key, Supplier<T> supplier) {
        T t = get(key);
        if (t != null) {
            return t;
        } else {
            store(key, supplier);
            return get(key);
        }
    }

    public <T> T get(Class<T> key) {
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
        for (Entry<Class<?>, Object> entry : realized.entrySet()) {
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
