package teaselib.core.util;

import teaselib.Toys;

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
}
