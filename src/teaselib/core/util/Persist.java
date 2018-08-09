package teaselib.core.util;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Persist {
    public static final String PERSISTED_STRING_SEPARATOR = " ";

    private static final String CLASS_NAME = "Class=";
    private static final String ELEMENT_SEPARATOR = " ";
    private static final String CLASS_VALUE_SEPARATOR = ";";
    private static final String STRING_REPRESENTATION = "Value=";

    public interface Persistable {
        List<String> persisted();
    }

    @FunctionalInterface
    public interface Factory {
        Object get(Class<?> clazz);
    }

    public static class Storage {
        private final Iterator<String> field;
        private final Optional<Factory> factory;

        public Storage(List<String> fields) {
            this.field = fields.iterator();
            this.factory = Optional.empty();
        }

        public Storage(List<String> fields, Factory factory) {
            this.field = fields.iterator();
            this.factory = Optional.of(factory);
        }

        // TODO private - used by PersistTest - replace with call to other constructor
        Storage(String serialized) {
            this(fields(serialized));
        }

        public <T> T next() {
            return Persist.from(field.next());
        }

        private static List<String> fields(String serialized) {
            return Arrays.asList(serialized.split(" "));
        }

        public boolean hasNext() {
            return field.hasNext();
        }

        @SuppressWarnings("unchecked")
        public <T> T getInstance(Class<T> clazz) {
            if (factory.isPresent()) {
                return (T) factory.get().get(clazz);
            } else {
                throw new IllegalArgumentException("Provide class factory for " + clazz.getName());
            }
        }
    }

    public static String persist(Object persistable) {
        if (persistable instanceof Persistable) {
            List<String> values = ((Persistable) persistable).persisted();
            return persistedInstance((Persistable) persistable, values);
        } else {
            String serializedObject = persistElement(persistable);
            if (canPersistObject(serializedObject)) {
                return persistedInstance(persistable, serializedObject);
            } else {
                throw new UnsupportedOperationException(
                        "String serialized objects with white space aren't supported yet.");
            }
        }

    }

    private static String persistedInstance(Persistable object, List<String> values) {
        return persistedInstance(object, joinPersistedValues(values));
    }

    public static String persistedInstance(Class<? extends Persistable> clazz, List<String> values) {
        return persistedInstance(clazz.getName(), values);
    }

    public static String persistedInstance(String className, List<String> values) {
        return persistedInstance(className,
                joinPersistedValues(values.stream().map(Persist::persist).collect(Collectors.toList())));
    }

    private static String persistedInstance(Object object, String persistedValues) {
        return persistedInstance(object.getClass().getName(), persistedValues);
    }

    public static String persistedInstance(String className, String persistedValues) {
        return CLASS_NAME + className + CLASS_VALUE_SEPARATOR + STRING_REPRESENTATION + persistedValues;
    }

    private static String persistElement(Object persistable) {
        if (persistable instanceof Collection) {
            throw new UnsupportedOperationException("Collections are not supported");
        } else {
            return persistable.toString();
        }
    }

    private static String joinPersistedValues(List<String> list) {
        StringBuilder serialized = new StringBuilder();
        boolean insertSeparator = false;
        for (String string : list) {
            if (insertSeparator) {
                serialized.append(ELEMENT_SEPARATOR);
            }
            serialized.append(string);
            insertSeparator = true;
        }
        return serialized.toString();
    }

    private static boolean canPersistObject(String serializedObject) {
        return !serializedObject.contains(PERSISTED_STRING_SEPARATOR);
    }

    public static <T> T from(String persisted) {
        return deserialize(className(persisted), persistedValue(persisted), Optional.empty());
    }

    public static <T> T from(String persisted, Factory factory) {
        return deserialize(className(persisted), persistedValue(persisted), Optional.of(factory));
    }

    // TODO package or private - used by StateImpl
    public static String className(String persisted) {
        String className = persisted.substring(CLASS_NAME.length(),
                persisted.indexOf(CLASS_VALUE_SEPARATOR + STRING_REPRESENTATION));
        return className;
    }

    // TODO package or private - used by StateImpl
    public static String persistedValue(String persisted) {
        return persisted.substring(CLASS_NAME.length() + className(persisted).length() + CLASS_VALUE_SEPARATOR.length()
                + STRING_REPRESENTATION.length());
    }

    @SuppressWarnings("unchecked")
    private static <T> T deserialize(String className, String stringRepresentation, Optional<Factory> factory) {
        try {
            Class<?> clazz = Class.forName(className);
            if (clazz.isEnum()) {
                @SuppressWarnings("rawtypes")
                Class<Enum> enumClass = (Class<Enum>) clazz;
                Enum<?> enumValue = Enum.valueOf(enumClass, stringRepresentation);
                return (T) enumValue;
            } else if (Persist.Persistable.class.isAssignableFrom(clazz)) {
                Constructor<?> constructor = clazz.getDeclaredConstructor(Persist.Storage.class);
                List<String> fields = Storage.fields(stringRepresentation);
                Storage storage = factory.isPresent() ? new Storage(fields, factory.get()) : new Storage(fields);
                return (T) constructor.newInstance(storage);
            } else {
                Constructor<?> constructor = clazz.getDeclaredConstructor(String.class);
                return (T) constructor.newInstance(stringRepresentation);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Cannot restore " + className + ":" + stringRepresentation, e);
        }
    }

    public static boolean isPersistedString(String string) {
        return string.startsWith(CLASS_NAME);
    }
}
