package teaselib.core.util;

import java.lang.reflect.Constructor;

public class Persist {
    public static final String PERSISTED_STRING_SEPARATOR = " ";

    private static final String CLASS_NAME = "Class=";
    private static final String SEPARATOR = ";";
    private static final String STRING_REPRESENTATION = "Value=";

    public interface Persistable {
        String persistToString();

        void restoreFromString();
    }

    public static String persist(Object persistable) {
        String serializedObject = persistable instanceof Persistable ? ((Persistable) persistable).persistToString()
                : persistable.toString();
        if (canPersistObject(serializedObject)) {
            return CLASS_NAME + persistable.getClass().getName() + SEPARATOR + STRING_REPRESENTATION + serializedObject;
        } else {
            throw new UnsupportedOperationException("String serialized objects with white space aren't supported yet.");
        }
    }

    private static boolean canPersistObject(String serializedObject) {
        return !serializedObject.contains(PERSISTED_STRING_SEPARATOR);
    }

    public static <T> T from(String serializedObject) {
        String className = serializedObject.substring(CLASS_NAME.length(),
                serializedObject.indexOf(SEPARATOR + STRING_REPRESENTATION));
        String stringRepresentation = serializedObject.substring(
                CLASS_NAME.length() + className.length() + SEPARATOR.length() + STRING_REPRESENTATION.length());
        return deserialize(className, stringRepresentation);
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
