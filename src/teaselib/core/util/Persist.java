package teaselib.core.util;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Persist {
    private static final String PERSISTED_STRING_SEPARATOR = " ";

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
        public Storage(String serialized) {
            this(fields(serialized));
        }

        public <T> T next() {
            return Persist.from(field.next());
        }

        private static List<String> fields(String persistedValues) {
            return fromList(persistedValues);
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

    public static String persist(Collection<?> persistable) {
        String className = persistable.getClass().getName();
        Collection<?> values = persistable;
        // TODO Resolve code duplication with persistedInstance(String className, List<String> values)
        List<String> persistedValues = values.stream().map(Persist::persist).collect(Collectors.toList());
        return persistedInstance(className, joinPersistedValues(persistedValues));
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

    // TODO Generalize since just needed by ActionState -> any type, similar to persist(Collection<?>
    public static String persistedInstance(Class<? extends Persistable> clazz, List<String> values) {
        return persistedInstance(clazz.getName(), values);
    }

    public static String persistedInstance(String className, List<String> values) {
        List<String> persistedValues = values.stream().map(Persist::persist).collect(Collectors.toList());
        return persistedInstance(className, joinPersistedValues(persistedValues));
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
        serialized.append(list.size());
        for (String string : list) {
            serialized.append(ELEMENT_SEPARATOR);
            serialized.append(string);
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

    // TODO Review, as this just splits the list values, but does not restore the list itself
    public static List<String> fromList(String persistedList) {
        String[] array = splitTopLevelElements(persistedList);
        int size = Integer.parseInt(array[0]);
        String[] values = hackAroundNestedObjects(array);
        if (size != values.length) {
            throw new IllegalArgumentException(
                    "Persisted value list size mismatch: " + size + "!= sizeof" + Arrays.toString(values));
        }
        return Arrays.asList(values);
    }

    private static String[] hackAroundNestedObjects(String[] array) {
        String[] valuesFlat = Arrays.copyOfRange(array, 1, array.length);

        // Hack around until we have a good solution for nested objects
        List<String> topLevel = new ArrayList<>();
        Iterator<String> value = Arrays.asList(valuesFlat).iterator();
        while (value.hasNext()) {
            String string = value.next();
            if (string.equals("Class=teaselib.util.ItemImpl;Value=2")) {
                StringBuilder nested = new StringBuilder();
                nested.append(string);
                nested.append(PERSISTED_STRING_SEPARATOR);
                nested.append(value.next());
                nested.append(PERSISTED_STRING_SEPARATOR);
                nested.append(value.next());
                topLevel.add(nested.toString());
            } else {
                topLevel.add(string);
            }
        }

        return topLevel.toArray(new String[0]);
    }

    private static String[] splitTopLevelElements(String persistedList) {
        String[] array = persistedList.split(Persist.PERSISTED_STRING_SEPARATOR);
        return array;
    }

    // TODO package or private - used by StateImpl
    public static String className(String persisted) {
        return persisted.substring(CLASS_NAME.length(),
                persisted.indexOf(CLASS_VALUE_SEPARATOR + STRING_REPRESENTATION));
    }

    // TODO package or private - used by StateImpl
    public static String persistedValue(String persisted) {
        return persisted.substring(CLASS_NAME.length() + className(persisted).length() + CLASS_VALUE_SEPARATOR.length()
                + STRING_REPRESENTATION.length());
    }

    @SuppressWarnings("unchecked")
    private static <T> T deserialize(String className, String persisted, Optional<Factory> factory) {
        try {
            Class<?> clazz = Class.forName(className);
            if (clazz.isEnum()) {
                @SuppressWarnings("rawtypes")
                Class<Enum> enumClass = (Class<Enum>) clazz;
                // One field exactly
                Enum<?> enumValue = Enum.valueOf(enumClass, persisted);
                return (T) enumValue;
            } else if (Iterable.class.isAssignableFrom(clazz)) {
                // Probably all fields of the string
                List<String> values = Persist.fromList(persisted);
                List<?> restored = new ArrayList<>();
                for (String value : values) {
                    restored.add(from(value));
                }
                return (T) restored;
            } else if (Persist.Persistable.class.isAssignableFrom(clazz)) {
                Constructor<?> constructor = clazz.getDeclaredConstructor(Persist.Storage.class);
                // n fields, followed by more fields
                List<String> fields = Storage.fields(persisted);
                Storage storage = factory.isPresent() ? new Storage(fields, factory.get()) : new Storage(fields);
                return (T) constructor.newInstance(storage);
            } else { // default string constructor
                Constructor<?> constructor = clazz.getDeclaredConstructor(String.class);
                return (T) constructor.newInstance(persisted);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Cannot restore " + className + ":" + persisted, e);
        }
    }

    public static boolean isPersistedString(String string) {
        return string.startsWith(CLASS_NAME);
    }
}
