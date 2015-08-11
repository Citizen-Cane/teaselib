package teaselib.hosts;

import java.util.Locale;

import ss.IScript;
import teaselib.Clothing;
import teaselib.Toys;
import teaselib.core.Persistence;
import teaselib.util.Item;
import teaselib.util.Value;

public class SexScriptsStatePersistence implements Persistence {

    private static final String FALSE = "false";
    private static final String TRUE = "true";

    private final ss.IScript host;

    public SexScriptsStatePersistence(IScript host) {
        this.host = host;
    }

    @Override
    public boolean has(String name) {
        return host.loadString(name) != null;
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

    @Override
    public void clear(String name) {
        host.save(name, null);
    }

    @Override
    public Item get(Toys item) {
        return getToy(item.name());
    }

    @Override
    public Item get(Clothing item) {
        return getClothingItem(item.name());
    }

    @Override
    public Item getToy(String name) {
        final String displayName = Value.createDisplayName(name);
        if (Toys.Ball_Gag.name().equals(name)) {
            name = "Ballgag";
        }
        return new Item("toys." + name.toLowerCase(), displayName, this);
    }

    @Override
    public Item getClothingItem(String item) {
        final String displayName = Value.createDisplayName(item);
        return new Item("clothes." + item.toLowerCase(), displayName, this);
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
