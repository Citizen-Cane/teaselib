package teaselib.core.util;

public final class ReflectionUtils {
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
}
