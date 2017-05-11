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

    public static String getClass(String qualifiedName) {
        return qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
    }

    public static String getName(String qualifiedName) {
        return qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1);
    }

    @SuppressWarnings({ "unchecked", "cast", "rawtypes" })
    public static Enum<?> getEnum(String qualifiedName) throws ClassNotFoundException {
        Class<?> enumClass = Class.forName(ReflectionUtils.getClass(qualifiedName));
        return (Enum<?>) Enum.valueOf((Class<? extends Enum>) enumClass, ReflectionUtils.getName(qualifiedName));
    }

    public static List<Enum<?>> getEnums(List<String> qualifiedNames) throws ClassNotFoundException {
        List<Enum<?>> enums = new ArrayList<Enum<?>>(qualifiedNames.size());
        for (String qualifiedName : qualifiedNames) {
            enums.add(getEnum(qualifiedName));
        }
        return enums;
    }

    public static List<Enum<?>> getEnums(String[] qualifiedName) throws ClassNotFoundException {
        return getEnums(Arrays.asList(qualifiedName));
    }

}
