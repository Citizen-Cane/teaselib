package teaselib.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ReflectionUtils {
    public static String classParentName(Class<?> clazz) {
        String path = normalizeClassName(clazz);
        return path.substring(0, path.lastIndexOf("."));
    }

    public static String classSimpleName(Class<?> clazz) {
        String path = normalizeClassName(clazz);
        return path.substring(path.lastIndexOf(".") + 1);
    }

    public static String classParentName(Object object) {
        String path = normalizeClassName(object.getClass());
        return path.substring(0, path.lastIndexOf("."));
    }

    public static String classSimpleName(Object object) {
        String path = normalizeClassName(object.getClass());
        return path.substring(path.lastIndexOf(".") + 1);
    }

    public static String normalizeClassName(Class<?> clazz) {
        return clazz.getName().replace("$", ".");
    }

    public static String normalizeClassName(String className) {
        return className.replace("$", ".");
    }

    public static String asAbsolutePath(String className) {
        return "/" + className.replace(".", "/") + "/";
    }

    public static String asClassLoaderCompatiblePath(String className) {
        return className.replace(".", "/") + "/";
    }

    public static String getEnumClass(String enumValue) {
        return enumValue.substring(0, enumValue.lastIndexOf("."));
    }

    public static String getEnmumValue(String enumValue) {
        return enumValue.substring(enumValue.lastIndexOf(".") + 1);
    }

    @SuppressWarnings({ "unchecked", "cast", "rawtypes" })
    public static Enum<?> getEnum(String enumValue) throws ClassNotFoundException {
        Class<?> enumClass = Class.forName(ReflectionUtils.getEnumClass(enumValue));
        return (Enum<?>) Enum.valueOf((Class<? extends Enum>) enumClass, ReflectionUtils.getEnmumValue(enumValue));
    }

    public static List<Enum<?>> getEnums(List<String> enumValues) throws ClassNotFoundException {
        List<Enum<?>> enums = new ArrayList<Enum<?>>(enumValues.size());
        for (String enumValue : enumValues) {
            enums.add(getEnum(enumValue));
        }
        return enums;
    }

    public static List<Enum<?>> getEnums(String[] enumValues) throws ClassNotFoundException {
        return getEnums(Arrays.asList(enumValues));
    }

}
