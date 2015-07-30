package teaselib.hosts;

import java.util.Locale;

import ss.IScript;
import teaselib.persistence.Clothing;
import teaselib.persistence.Item;
import teaselib.persistence.Toys;
import teaselib.persistence.Value;

public class SexScriptsStatePersistence implements Persistence {

    private static final String FALSE = "false";
    private static final String TRUE = "true";

    private final ss.IScript host;

    public SexScriptsStatePersistence(IScript host) {
        this.host = host;
    }

    @Override
    public String get(String name) {
        String value = host.loadString(name);
        return value;
    }

    @Override
    public void set(String name, String value) {
        host.save(name, value);
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
    public void set(String name, boolean value) {
        set(name, value ? TRUE : FALSE);
    }

    // @Override
    // public void write(String name, int value) {
    // host.save(root + "." + name, new Integer(value));
    // }

    // @Override
    // public int read(String name, int defaultValue) {
    // Float value = host.loadFloat(root + "." + name);
    // if (value != 0)
    // {
    // return value.intValue();
    // }
    // else
    // {
    // return defaultValue;
    // }
    // }

    @Override
    public Item get(Toys item) {
        return getToy(item.toString());
    }

    @Override
    public Item get(Clothing item) {
        return getClothingItem(item.toString());
    }

    @Override
    public Item getToy(String name) {
        if (Toys.Ball_Gag.toString().equals(name)) {
            name = "Ballgag";
        }
        final String displayName = Value.createDisplayName(name);
        return new Item("toys." + name.toLowerCase(), displayName, this);
    }

    @Override
    public Item getClothingItem(String item) {
        final String name = item.toString();
        final String displayName = Value.createDisplayName(item.toString());
        return new Item("clothes." + name.toLowerCase(), displayName, this);
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
