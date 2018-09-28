package teaselib.core.util;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Persist {
    private static final String PERSISTED_STRING_SEPARATOR = " ";

    private static final String CLASS_NAME = "Class=";
    private static final String ELEMENT_SEPARATOR = " ";
    private static final String CLASS_VALUE_SEPARATOR = ";";
    private static final String STRING_REPRESENTATION_SINGLE_VALUE = "Value=";
    private static final String STRING_REPRESENTATION_MULTIPLE_VALUES_LEADER = "Values[";
    private static final String STRING_REPRESENTATION_MULTIPLE_VALUES_TRAILER = "]=";

    public static class PersistedObject {
        String className;
        List<String> values;

        public PersistedObject(String persisted) {
            this.className = className(persisted);
            String persistedValue = persistedValue(persisted);
            this.values = flatSplit(persistedValue);
        }

        PersistedObject(String className, List<String> values) {
            this.className = className;
            this.values = values;
        }

        PersistedObject(Object instance) {
            this.className = instance.getClass().getName();

            if (instance instanceof Persistable) {
                this.values = ((Persistable) instance).persisted();
            } else if (instance instanceof Collection) {
                Collection<?> persistable = (Collection<?>) instance;
                this.values = persistable.stream().map(Persist::persist).collect(Collectors.toList());
            } else {
                values = Collections.singletonList(persistElement(instance));
            }
        }

        public static String className(String persisted) {
            if (persisted.startsWith(CLASS_NAME)) {
                return persisted.substring(CLASS_NAME.length(), persisted.indexOf(CLASS_VALUE_SEPARATOR));
            } else {
                throw new IllegalArgumentException("Class name not found  '" + persisted + "'");
            }
        }

        public static String persistedValue(String persisted) {
            if (persisted.startsWith(CLASS_NAME)) {
                int index = persisted.indexOf(CLASS_VALUE_SEPARATOR);
                if (index > CLASS_NAME.length()) {
                    return persisted.substring(index + 1);
                }
            }

            throw new IllegalArgumentException("Class name not found  '" + persisted + "'");
        }

        public static int valueCount(String persistedValue) {
            if (persistedValue.startsWith(STRING_REPRESENTATION_SINGLE_VALUE)) {
                return 1;
            } else if (persistedValue.startsWith(STRING_REPRESENTATION_MULTIPLE_VALUES_LEADER)) {
                return Integer.parseInt(persistedValue.substring(STRING_REPRESENTATION_MULTIPLE_VALUES_LEADER.length(),
                        persistedValue.indexOf(STRING_REPRESENTATION_MULTIPLE_VALUES_TRAILER)));
            } else {
                throw new IllegalArgumentException("Value count not found in '" + persistedValue + "'");
            }
        }

        public static String value(String persistedValue) {
            if (persistedValue.startsWith(STRING_REPRESENTATION_SINGLE_VALUE)) {
                return persistedValue.substring(STRING_REPRESENTATION_SINGLE_VALUE.length());
            } else if (persistedValue.startsWith(STRING_REPRESENTATION_MULTIPLE_VALUES_LEADER)) {
                int index = persistedValue.indexOf(STRING_REPRESENTATION_MULTIPLE_VALUES_TRAILER);
                return persistedValue.substring(index + STRING_REPRESENTATION_MULTIPLE_VALUES_TRAILER.length());
            } else {
                throw new IllegalArgumentException("Value not found in '" + persistedValue + "'");
            }
        }

        @Override
        public String toString() {
            StringBuilder persisted = new StringBuilder();
            persisted.append(CLASS_NAME);
            persisted.append(className);
            persisted.append(CLASS_VALUE_SEPARATOR);

            if (values.size() > 1) {
                persisted.append(STRING_REPRESENTATION_MULTIPLE_VALUES_LEADER);
                persisted.append(values.size());
                persisted.append(STRING_REPRESENTATION_MULTIPLE_VALUES_TRAILER);
            } else {
                persisted.append(STRING_REPRESENTATION_SINGLE_VALUE);
            }

            persisted.append(joinPersistedValues(values));

            return persisted.toString();
        }

        public <T> T toInstance() {
            return deserialize(className, values, Optional.empty());
        }

        public <T> T toInstance(Factory factory) {
            return deserialize(className, values, Optional.of(factory));
        }

        public List<String> toValues() {
            return values;
        }

        public Storage toStorage() {
            return new Persist.Storage(values);
        }

        private static List<String> flatSplit(String values) {
            int valueCount = valueCount(values);
            if (valueCount > 1) {
                List<String> flattened = flatten(split(value(values)));
                if (valueCount != flattened.size()) {
                    throw new IllegalArgumentException(
                            "Persisted value list size mismatch: " + valueCount + "!= sizeof" + values);
                }
                return flattened;
            } else {
                return Collections.singletonList(value(values));
            }
        }

        private static String[] split(String values) {
            return values.split(Persist.PERSISTED_STRING_SEPARATOR);
        }

        private static List<String> flatten(String[] splitted) {
            List<String> flattened = new ArrayList<>();
            Iterator<String> value = Arrays.asList(splitted).iterator();
            while (value.hasNext()) {
                String persisted = value.next();
                int valueCount = PersistedObject.valueCount(persistedValue(persisted));
                if (valueCount > 1) {
                    StringBuilder nested = new StringBuilder();
                    nested.append(persisted);
                    for (int i = 0; i < valueCount - 1; i++) {
                        nested.append(PERSISTED_STRING_SEPARATOR);
                        nested.append(value.next());
                    }
                    flattened.add(nested.toString());
                } else {
                    flattened.add(persisted);
                }
            }

            return flattened;
        }

        @SuppressWarnings("unchecked")
        private static <T> T deserialize(String className, List<String> persistedValues, Optional<Factory> factory) {
            try {
                Class<?> clazz = Class.forName(className);
                if (clazz.isEnum()) {
                    @SuppressWarnings("rawtypes")
                    Class<Enum> enumClass = (Class<Enum>) clazz;
                    // One field exactly
                    Enum<?> enumValue = Enum.valueOf(enumClass, persistedValues.get(0));
                    return (T) enumValue;
                } else if (Iterable.class.isAssignableFrom(clazz)) {
                    List<?> restored = new ArrayList<>();
                    for (String value : persistedValues) {
                        restored.add(new PersistedObject(value).toInstance());
                    }
                    return (T) restored;
                } else if (Persist.Persistable.class.isAssignableFrom(clazz)) {
                    Constructor<?> constructor = clazz.getDeclaredConstructor(Persist.Storage.class);
                    // n fields, followed by more fields
                    Storage storage = factory.isPresent() ? new Storage(persistedValues, factory.get())
                            : new Storage(persistedValues);
                    return (T) constructor.newInstance(storage);
                } else { // default string constructor
                    Constructor<?> constructor = clazz.getDeclaredConstructor(String.class);
                    return (T) constructor.newInstance(persistedValues.get(0));
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException("Cannot restore " + className + ":" + persistedValues, e);
            }
        }

        private static String persistElement(Object persistable) {
            if (persistable instanceof Persistable) {
                throw new IllegalStateException("Persistables aren't supported intrinsicly");
            } else if (persistable instanceof Collection) {
                throw new UnsupportedOperationException("Collections aren't supported intrinsicly");
            } else {
                String persisted = persistable.toString();
                if (canPersistObject(persisted)) {
                    return persisted;
                } else {
                    throw new UnsupportedOperationException("White space in values is not supported");
                }
            }
        }

        private static boolean canPersistObject(String serializedObject) {
            return !serializedObject.contains(PERSISTED_STRING_SEPARATOR);
        }

        private static String joinPersistedValues(List<String> list) {
            StringBuilder serialized = new StringBuilder();
            boolean appendSeparator = false;
            for (String string : list) {
                if (appendSeparator) {
                    serialized.append(ELEMENT_SEPARATOR);
                }
                serialized.append(string);
                appendSeparator = true;
            }
            return serialized.toString();
        }
    }

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

        public <T> T next() {
            return Persist.from(field.next());
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

    public static boolean isPersistedString(String string) {
        return string.startsWith(CLASS_NAME);
    }
}
