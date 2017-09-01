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
        private final Iterator<String> elements;

        public Storage(List<String> elements) {
            super();
            this.elements = elements.iterator();
        }

        public Storage(String serialized) {
            this(split(serialized));
        }

        public <T> T next() {
            return Persist.from(elements.next());
        }
    }

    public static String persist(Object persistable) {
        String serializedObject = persistable instanceof Persistable ? join(((Persistable) persistable).persisted())
                : persistElement(persistable);
        if (canPersistObject(serializedObject)) {
            return CLASS_NAME + persistable.getClass().getName() + CLASS_VALUE_SEPARATOR + STRING_REPRESENTATION
                    + serializedObject;
        } else {
            throw new UnsupportedOperationException("String serialized objects with white space aren't supported yet.");
        }
    }

    private static String persistElement(Object persistable) {
        if (persistable instanceof Collection) {
            throw new UnsupportedOperationException("TODO recursive collection persistence");
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

    private static List<String> split(String serialized) {
        return Arrays.asList(serialized.split(" "));
    }

    private static boolean canPersistObject(String serializedObject) {
        return !serializedObject.contains(PERSISTED_STRING_SEPARATOR);
    }

    public static <T> T from(String persisted) {
        String className = className(persisted);
        String stringRepresentation = persistedValue(persisted);
        return deserialize(className, stringRepresentation);
    }

    public static String className(String persisted) {
        String className = persisted.substring(CLASS_NAME.length(),
                persisted.indexOf(CLASS_VALUE_SEPARATOR + STRING_REPRESENTATION));
        return className;
    }

    public static String persistedValue(String persisted) {
        String stringRepresentation = persisted.substring(CLASS_NAME.length() + className(persisted).length()
                + CLASS_VALUE_SEPARATOR.length() + STRING_REPRESENTATION.length());
        return stringRepresentation;
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
            } else {
                Constructor<?> constructor = clazz.getConstructor(String.class);
                return (T) constructor.newInstance(stringRepresentation);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Cannot restore " + className + ":" + stringRepresentation, e);
        }
    }
}
