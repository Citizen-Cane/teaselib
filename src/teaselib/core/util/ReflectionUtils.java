package teaselib.core.util;

public final class ReflectionUtils {

    private ReflectionUtils() {
    }

    public static String classParentName(Class<?> clazz) {
        String path = normalizedClassName(clazz);
        return path.substring(0, path.lastIndexOf('.'));
    }

    public static String classSimpleName(Class<?> clazz) {
        String path = normalizedClassName(clazz);
        return path.substring(path.lastIndexOf('.') + 1);
    }

    public static String classParentName(Object object) {
        String path = normalizedClassName(object.getClass());
        return path.substring(0, path.lastIndexOf('.'));
    }

    public static String classSimpleName(Object object) {
        String path = normalizedClassName(object.getClass());
        return path.substring(path.lastIndexOf('.') + 1);
    }

    public static String normalizedClassName(Class<?> clazz) {
        return clazz.getName().replace('$', '.');
    }

    public static String qualified(Class<?> clazz, String name) {
        return normalizedClassName(clazz) + "." + name;
    }

    public static String qualified(String namespace, Class<?> clazz, String name) {
        return namespace + "." + clazz.getSimpleName() + "." + name;
    }

    public static String qualified(String namespace, String name) {
        return namespace + "." + name;
    }

    public static String absolutePath(Package p) {
        return "/" + p.getName().replace('.', '/') + '/';
    }

    public static String absolutePath(Class<?> clazz) {
        return absolutePath(clazz.getPackage());
    }

    public static String packagePath(Class<?> clazz) {
        return clazz.getPackage().getName().replace(".", "/") + "/";
    }

    public static String asClassLoaderCompatiblePath(Class<?> clazz) {
        return clazz.getName().replace('.', '/') + '/';
    }

    public static <T extends Enum<?>> T getEnum(QualifiedItem qualifiedItem) {
        String className = qualifiedItem.namespace();
        try {
            @SuppressWarnings("unchecked")
            Class<T> enumClass = (Class<T>) Class.forName(className);
            return getEnum(enumClass, qualifiedItem);
        } catch (ClassNotFoundException e) {
            try {
                @SuppressWarnings("unchecked")
                Class<T> nestedClass = (Class<T>) Class.forName(nestedClass(className));
                return getEnum(nestedClass, qualifiedItem);
            } catch (ClassNotFoundException e1) {
                throw new IllegalArgumentException("Not an enum:" + qualifiedItem, e);
            }
        }
    }

    private static String nestedClass(String className) {
        int index = className.lastIndexOf('.');
        if (index < 0) {
            throw new IllegalArgumentException();
        } else {
            return className.substring(0, index) + "$" + className.substring(index + 1);
        }
    }

    public static <T extends Enum<?>> T getEnum(Class<T> enumClass, String qualifiedName) {
        return getEnum(enumClass, QualifiedItem.nameOf(qualifiedName));
    }

    public static <T extends Enum<?>> T getEnum(Class<T> enumClass, QualifiedItem qualifiedItem) {
        for (T value : enumClass.getEnumConstants()) {
            if (value.name().equalsIgnoreCase(qualifiedItem.name())) {
                return value;
            }
        }
        throw new IllegalArgumentException(enumClass.getName() + ": undefined enum constant" + ": " + qualifiedItem);
    }

    public static String qualifiedName(Enum<?> value) {
        return ReflectionUtils.normalizedClassName(value.getClass()) + '.' + value.name();
    }
}
