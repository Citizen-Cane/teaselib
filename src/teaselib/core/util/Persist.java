package teaselib.core.util;

import java.util.List;

public class Persist {
    /**
     * @author citizen-cane
     *
     *         Implementing also requires to implements a constructor {@link Persistable}).<init>({@link Storage})
     */
    public interface Persistable {
        // TODO Storage instance as parameter or return value
        List<String> persisted();
    }

    @FunctionalInterface
    public interface Factory {
        Object get(Class<?> clazz);
    }

    public static String persist(Object instance) {
        return new PersistedObject(instance).toString();
    }

    public static String persistValues(Object instance) {
        return new PersistedObject(instance).toValueString();
    }

    public static String persistedInstance(Class<? extends Persistable> clazz, List<String> values) {
        return new PersistedObject(clazz, new PersistedObject(values).toValues()).toString();
    }

    public static <T> T from(String persisted) {
        return new PersistedObject(persisted).toInstance();
    }

    public static <T> T from(Class<?> clazz, String persistedValue) {
        return new PersistedObject(clazz, persistedValue).toInstance();
    }

    public static <T> T from(String persisted, Factory factory) {
        return new PersistedObject(persisted).toInstance(factory);
    }
}
