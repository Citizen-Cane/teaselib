package teaselib.core.util;

import teaselib.Toys;
import teaselib.core.Persistence;

public class PropertyNameMapping {
    public static final PropertyNameMapping PassThrough = new PropertyNameMapping();

    public static final String DefaultDomain = new String();
    public static final String None = new String();

    protected static final String[] StrippedPackageNames = { "teaselib",
            "teaselib.scripts" };

    /**
     * @param domain
     * @param path
     * @param name
     * @return
     */
    public String stripPath(String domain, String path, String name) {
        for (String packageName : StrippedPackageNames) {
            String string = packageName + ".";
            if (path.startsWith(string)) {
                path = path.substring(string.length());
            }
        }
        return path;
    }

    /**
     * @param domain
     * @param path
     * @param name
     * @return
     */
    public String mapDomain(String domain, String path, String name) {
        return domain;
    }

    /**
     * @param domain
     * @param path
     * @param name
     * @return
     */
    public String mapPath(String domain, String path, String name) {
        return path;
    }

    /**
     * @param domain
     * @param path
     * @param name
     * @return
     */
    public String mapName(String domain, String path, String name) {
        return name;
    }

    public String buildPath(String... parts) {
        if (parts.length == 0) {
            return None;
        } else {
            StringBuilder path = new StringBuilder(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                if (!None.equalsIgnoreCase(parts[i])) {
                    if (path.length() > 0) {
                        path.append(".");
                    }
                    path.append(parts[i]);
                }
            }
            return path.toString().replace("$", ".");
        }
    }

    public static String reduceToSimpleName(String name, Class<Toys> clazz) {
        String className = clazz.getName();
        if (name.startsWith(className)) {
            return clazz.getSimpleName() + "."
                    + name.substring(className.length());
        }
        return name;
    }

    public boolean has(String name, Persistence persistence) {
        return persistence.has(name);
    }

    public String get(String name, Persistence persistence) {
        return persistence.get(name);
    }

    public boolean getBoolean(String name, Persistence persistence) {
        return persistence.getBoolean(name);
    }

    public void set(String name, String value, Persistence persistence) {
        persistence.set(name, value);
    }

    public void set(String name, boolean value, Persistence persistence) {
        persistence.set(name, value);
    }

    public void clear(String name, Persistence persistence) {
        persistence.clear(name);
    }

}
