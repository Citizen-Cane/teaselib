package teaselib.core.util;

import java.lang.reflect.Constructor;

public class Persist {
    public static final String CLASS_NAME = "Class:";
    public static final String SEPARATOR = ";";
    public static final String STRING_REPRESENTATION = "Value:";

    public static String to(Object toStringSerializable) {
        return CLASS_NAME + toStringSerializable.getClass().getName()
                + SEPARATOR + STRING_REPRESENTATION
                + toStringSerializable.toString();
    }

    public static <T> T from(String serializedObject) {
        String className = serializedObject.substring(CLASS_NAME.length(),
                serializedObject.indexOf(SEPARATOR + STRING_REPRESENTATION));
        String stringRepresentation = serializedObject
                .substring(CLASS_NAME.length() + className.length()
                        + SEPARATOR.length() + STRING_REPRESENTATION.length());
        return deserialize(className, stringRepresentation);
    }

    @SuppressWarnings("unchecked")
    private static <T> T deserialize(String className,
            String stringRepresentation) {
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getConstructor(String.class);
            return (T) constructor.newInstance(stringRepresentation);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(
                    "Cannot restore " + className + ":" + stringRepresentation,
                    e);
        }
    }
}