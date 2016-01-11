package teaselib.hosts;

import java.util.Locale;

import ss.IScript;
import teaselib.Toys;
import teaselib.core.Persistence;

public class SexScriptsStatePersistence implements Persistence {

    private static final String FALSE = "false";
    private static final String TRUE = "true";

    private final ss.IScript host;

    public SexScriptsStatePersistence(IScript host) {
        this.host = host;
    }

    @Override
    public boolean has(String name) {
        name = mapName(name);
        return host.loadString(name) != null;
    }

    @Override
    public boolean getBoolean(String name) {
        String value = get(name);
        if (value == null) {
            return false;
        } else {
            return !value.equals(FALSE);
        }
    }

    @Override
    public String get(String name) {
        name = mapName(name);
        String value = host.loadString(name);
        return value;
    }

    @Override
    public void set(String name, String value) {
        name = mapName(name);
        host.save(name, value);
    }

    @Override
    public void set(String name, boolean value) {
        name = mapName(name);
        set(name, value ? TRUE : FALSE);
    }

    @Override
    public void clear(String name) {
        name = mapName(name);
        host.save(name, null);
    }

    /**
     * TeaseLib uses a lot of standard properties instead of just plain strings,
     * 
     * @param name
     *            The name of a property
     * @return The actual property name or the original name
     */
    private static String mapName(String name) {
        if (("toys." + Toys.Ball_Gag.name()).equalsIgnoreCase(name)) {
            name = "toys.ballgag";
        }
        return name;
    }

    @Override
    public String get(TextVariable name, String locale) {
        if (name == TextVariable.Slave && defaultLanguageMatches(locale)) {
            return get("intro.name");
        }
        return get(name.value + "." + locale);
    }

    @Override
    public void set(TextVariable name, String locale, String value) {
        if (name == TextVariable.Slave && defaultLanguageMatches(locale)) {
            set("intro.name", value);
        } else {
            set(name.name() + "." + locale, value);
        }
    }

    private static boolean defaultLanguageMatches(String locale) {
        final Locale defaultLocale = Locale.getDefault();
        String defaultLanguage = defaultLocale.getLanguage();
        final String localeLanguage = new Locale(locale).getLanguage();
        final boolean languagesMatch = defaultLanguage.equals(localeLanguage);
        return languagesMatch;
    }
}
