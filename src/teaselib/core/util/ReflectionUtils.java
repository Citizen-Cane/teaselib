package teaselib.core.util;

public final class ReflectionUtils {

    private ReflectionUtils() {
    }

    public static String classParentName(Class<?> clazz) {
        String path = normalizeClassName(clazz);
        return path.substring(0, path.lastIndexOf('.'));
    }

    public static String classSimpleName(Class<?> clazz) {
        String path = normalizeClassName(clazz);
        return path.substring(path.lastIndexOf('.') + 1);
    }

    public static String classParentName(Object object) {
        String path = normalizeClassName(object.getClass());
        return path.substring(0, path.lastIndexOf('.'));
    }

    public static String classSimpleName(Object object) {
        String path = normalizeClassName(object.getClass());
        return path.substring(path.lastIndexOf('.') + 1);
    }

    public static String normalizeClassName(Class<?> clazz) {
        return clazz.getName().replace('$', '.');
    }

    public static String normalizeClassName(String className) {
        return className.replace("$", ".");
    }

    public static String asAbsolutePath(String className) {
        return (className.startsWith("/") ? "" : "/") + className.replace('.', '/') + '/';
    }

    public static String asAbsolutePath(Class<?> clazz) {
        return "/" + clazz.getName().replace('.', '/') + '/';
    }

    public static String getPackagePath(Class<?> clazz) {
        return clazz.getPackage().getName().replace(".", "/") + "/";
    }

    public static String asClassLoaderCompatiblePath(String className) {
        return className.replace('.', '/') + '/';
    }

    public static String asPath(String className) {
        return className.replace('.', '/');
    }

    public static String getClass(String qualifiedName) {
        return qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));
    }

    public static String getName(String qualifiedName) {
        return qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
    }

    public static Enum<?> getEnum(String qualifiedName) throws ClassNotFoundException {
        return getEnum(new QualifiedObject(qualifiedName));
    }

    public static Enum<?> getEnum(QualifiedItem<?> qualifiedItem) throws ClassNotFoundException {
        @SuppressWarnings("unchecked")
        Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) Class.forName(qualifiedItem.namespace());
        return getEnum(enumClass, qualifiedItem);
    }

    public static <T extends Enum<?>> T getEnum(Class<T> enumClass, String qualifiedName) {
        return getEnum(enumClass, QualifiedItem.nameOf(qualifiedName));
    }

    public static <T extends Enum<?>> T getEnum(Class<T> enumClass, QualifiedItem<?> qualifiedItem) {
        for (T value : enumClass.getEnumConstants()) {
            if (value.name().equalsIgnoreCase(qualifiedItem.name())) {
                return value;
            }
        }
        throw new IllegalArgumentException(enumClass.getName() + ": undefined enum constant" + ": " + qualifiedItem);
    }

    public static String qualifiedName(Enum<?> value) {
        return ReflectionUtils.normalizeClassName(value.getClass()) + '.' + value.name();
    }
}
