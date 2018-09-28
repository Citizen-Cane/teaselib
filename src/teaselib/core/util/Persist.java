package teaselib.core.util;

import java.util.List;

public class Persist {
    public interface Persistable {
        List<String> persisted();
    }

    @FunctionalInterface
    public interface Factory {
        Object get(Class<?> clazz);
    }

    public static String persist(Object instance) {
        return new PersistedObject(instance).toString();
    }

    public static String persistedInstance(Class<? extends Persistable> clazz, List<String> values) {
        return new PersistedObject(clazz.getName(), new PersistedObject(values).toValues()).toString();
    }

    public static <T> T from(String persisted) {
        return new PersistedObject(persisted).toInstance();
    }

    public static <T> T from(String persisted, Factory factory) {
        return new PersistedObject(persisted).toInstance(factory);
    }
}
