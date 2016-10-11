package teaselib.core.util;

import teaselib.Toys;

public class PropertyNameMapping {
    public static final PropertyNameMapping PassThrough = new PropertyNameMapping();

    public static final String None = new String();

    /**
     * @param domain
     * @param namespace
     * @param name
     * @return
     */
    public String mapDomain(String domain, String namespace, String name) {
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
     * @param namespace
     * @param name
     * @return
     */
    public String mapName(String domain, String namespace, String name) {
        return name;
    }

    public String buildPath(String... parts) {
        if (parts.length == 0) {
            return None;
        } else {
            StringBuilder path = new StringBuilder(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                if (!None.equalsIgnoreCase(parts[i])) {
                    path.append(".");
                    path.append(parts[i]);
                }
            }
            return path.toString();
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
