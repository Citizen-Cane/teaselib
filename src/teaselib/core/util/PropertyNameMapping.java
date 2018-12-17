package teaselib.core.util;

import java.io.IOException;
import java.util.Locale;

import teaselib.Actor;
import teaselib.Sexuality.Gender;
import teaselib.Toys;
import teaselib.core.Persistence;
import teaselib.core.TeaseLib;
import teaselib.core.UserItems;
import teaselib.util.PersistenceLogger;
import teaselib.util.TextVariables;

public class PropertyNameMapping implements Persistence {
    public static final String DefaultDomain = "";
    public static final String None = "";

    protected static final String[] StrippedPackageNames = { "teaselib", "teaselib.scripts" };

    private final Persistence persistence;

    public PropertyNameMapping(Persistence persistence) {
        this.persistence = new PersistenceLogger(persistence);
    }

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
            return clazz.getSimpleName() + "." + name.substring(className.length());
        }
        return name;
    }

    public boolean has(String name) {
        return persistence.has(name);
    }

    public String get(String name) {
        return persistence.get(name);
    }

    public boolean getBoolean(String name) {
        return persistence.getBoolean(name);
    }

    public void set(String name, String value) {
        persistence.set(name, value);
    }

    public void set(String name, boolean value) {
        persistence.set(name, value);
    }

    public void clear(String name) {
        persistence.clear(name);
    }

    public UserItems getUserItems(TeaseLib teaseLib) throws IOException {
        return persistence.getUserItems(teaseLib);
    }

    @Override
    public TextVariables getTextVariables(Locale locale) {
        return persistence.getTextVariables(locale);
    }

    @Override
    public Actor getDominant(Gender gender, Locale locale) {
        return persistence.getDominant(gender, locale);
    }

    @Override
    public PropertyNameMapping getNameMapping() {
        return this;
    }

}
