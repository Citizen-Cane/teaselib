package teaselib.core.util;

import java.net.URL;
import java.nio.file.Path;

public final class ReflectionUtils {

    private ReflectionUtils() {
    }

    public static String parent(Class<?> clazz) {
        String path = qualified(clazz);
        return parent(path);
    }

    public static String parent(String path) {
        return path.substring(0, path.lastIndexOf('.'));
    }

    public static String classSimpleName(Class<?> clazz) {
        String path = qualified(clazz);
        return path.substring(path.lastIndexOf('.') + 1);
    }

    public static String classParentName(Object object) {
        String path = qualified(object.getClass());
        return parent(path);
    }

    public static String qualified(Class<?> clazz) {
        return clazz.getName().replace('$', '.');
    }

    public static String qualified(Class<?> clazz, String name) {
        return qualified(clazz) + "." + name;
    }

    public static String qualified(Enum<?> namespace, String name) {
        return new QualifiedEnum(namespace) + "." + name;
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

    public static <T extends Enum<?>> T getEnum(QualifiedString qualifiedItem) {
        String className = qualifiedItem.namespace();
        try {
            @SuppressWarnings("unchecked")
            Class<T> enumClass = (Class<T>) Class.forName(className);
            return getEnum(enumClass, qualifiedItem);
        } catch (ClassNotFoundException e) {
            String nestedClassName1 = nestedClass(className);
            try {
                @SuppressWarnings("unchecked")
                Class<T> nestedClass = (Class<T>) Class.forName(nestedClassName1);
                return getEnum(nestedClass, qualifiedItem);
            } catch (ClassNotFoundException e1) {
                String nestedClassName2 = nestedClass(nestedClassName1);
                try {
                    @SuppressWarnings("unchecked")
                    Class<T> nestedClass = (Class<T>) Class.forName(nestedClassName2);
                    return getEnum(nestedClass, qualifiedItem);
                } catch (ClassNotFoundException e2) {
                    // TODO make this iterative
                    throw new IllegalArgumentException("Not an enum: " + qualifiedItem, e);
                }
            }
        }
    }

    private static String nestedClass(String className) {
        int index = className.lastIndexOf('.');
        if (index < 0) {
            throw new IllegalArgumentException(className);
        } else {
            return className.substring(0, index) + "$" + className.substring(index + 1);
        }
    }

    public static <T extends Enum<?>> T getEnum(Class<T> enumClass, QualifiedString qualifiedItem) {
        for (T value : enumClass.getEnumConstants()) {
            if (value.name().equalsIgnoreCase(qualifiedItem.name())) {
                return value;
            }
        }
        throw new IllegalArgumentException(enumClass.getName() + ": undefined enum constant" + ": " + qualifiedItem);
    }

    public static String qualifiedName(Enum<?> value) {
        return ReflectionUtils.qualified(value.getClass()) + '.' + value.name();
    }

    // Project path - blunt copy from ResourceLoader but with paths

    public static Path projectPath(Class<?> clazz) {
        String classFile = getClassFilePath(clazz);
        URL url = clazz.getClassLoader().getResource(classFile);
        String protocol = classLoaderCompatibleResourcePath(url.getProtocol().toLowerCase());
        if (protocol.equals("file")) {
            return projectPathFromFile(url, classFile);
        } else if (protocol.equals("jar")) {
            return projectParentPathFromJar(url);
        } else {
            throw new IllegalArgumentException("Unsupported protocol: " + url.toString());
        }
    }

    private static String getClassFilePath(Class<?> mainScript) {
        String classFile = "/" + mainScript.getName().replace(".", "/") + ".class";
        return classLoaderCompatibleResourcePath(classFile);
    }

    private static Path projectPathFromFile(URL url, String classFile) {
        String path = getUndecoratedPath(url);
        int classOffset = classFile.length();
        return Path.of(path.substring(0, path.length() - classOffset));
    }

    private static Path projectParentPathFromJar(URL url) {
        String path = getUndecoratedPath(url);
        return Path.of(path.substring("File:/".length(), path.indexOf(".jar!"))).getParent();
    }

    /**
     * {@link java.net.URL} paths have white space is escaped ({@code %20}), so to work with resources, these
     * decorations must be removed.
     * 
     * @param url
     *            The url to retrieve the undecorated path from.
     * @return A string containing the undecorated path part of the URL.
     */
    private static String getUndecoratedPath(URL url) {
        return classLoaderCompatibleResourcePath(url.getPath()).replace("%20", " ");
    }

    private static String classLoaderCompatibleResourcePath(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }

}
