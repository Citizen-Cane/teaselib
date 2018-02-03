package teaselib.core.util;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class Persist {
    public static final String PERSISTED_STRING_SEPARATOR = " ";

    private static final String CLASS_NAME = "Class=";
    private static final String ELEMENT_SEPARATOR = " ";
    private static final String CLASS_VALUE_SEPARATOR = ";";
    private static final String STRING_REPRESENTATION = "Value=";

    public interface Persistable {
        List<String> persisted();
    }

    public static class Storage {
        private final Iterator<String> field;

        private Storage(List<String> elements) {
            this.field = elements.iterator();
        }

        // TODO private - used by PersistTest
        Storage(String serialized) {
            this(split(serialized));
        }

        public <T> T next() {
            return Persist.from(field.next());
        }

        private static List<String> split(String serialized) {
            return Arrays.asList(serialized.split(" "));
        }

        public boolean hasNext() {
            return field.hasNext();
        }
    }

    public static String persist(Object persistable) {
        if (persistable instanceof Persistable) {
            return persistedInstance(persistable, join(((Persistable) persistable).persisted()));
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

    private static String persistedInstance(Object persisted, String serializedObject) {
        String className = persisted.getClass().getName();
        return CLASS_NAME + className + CLASS_VALUE_SEPARATOR + STRING_REPRESENTATION + serializedObject;
    }

    private static String persistElement(Object persistable) {
        if (persistable instanceof Collection) {
            throw new UnsupportedOperationException("Collections are not supported");
        } else {
            return persistable.toString();
        }
    }

    private static String join(List<String> persistToString) {
        StringBuilder serialized = new StringBuilder();
        boolean insertSeparator = false;
        for (String string : persistToString) {
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
        return deserialize(className(persisted), persistedValue(persisted));
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
    private static <T> T deserialize(String className, String stringRepresentation) {
        try {
            Class<?> clazz = Class.forName(className);
            if (clazz.isEnum()) {
                @SuppressWarnings("rawtypes")
                Class<Enum> enumClass = (Class<Enum>) clazz;
                Enum<?> enumValue = Enum.valueOf(enumClass, stringRepresentation);
                return (T) enumValue;
            } else if (Persist.Persistable.class.isAssignableFrom(clazz)) {
                Constructor<?> constructor = clazz.getDeclaredConstructor(Persist.Storage.class);
                return (T) constructor.newInstance(new Storage(stringRepresentation));
            } else {
                Constructor<?> constructor = clazz.getDeclaredConstructor(String.class);
                return (T) constructor.newInstance(stringRepresentation);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Cannot restore " + className + ":" + stringRepresentation, e);
        }
    }
}
