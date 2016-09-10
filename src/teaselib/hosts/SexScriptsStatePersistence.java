package teaselib.hosts;

import java.util.Locale;

import ss.IScript;
import teaselib.Actor;
import teaselib.Clothes;
import teaselib.Images;
import teaselib.Toys;
import teaselib.core.Persistence;
import teaselib.core.texttospeech.Voice;
import teaselib.util.TextVariables;

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
     * and those don't match the way SexScripts save it's properties.
     * 
     * @param name
     *            The name of a property
     * @return The actual property name or the original name
     */
    private static String mapName(String name) {
        if (name.contains(
                ("Male." + Clothes.class.getSimpleName()).toLowerCase()
                        + ".")) {
            name = name.substring(name.indexOf(
                    Clothes.class.getSimpleName().toLowerCase() + "."));
        } else if (name.contains(
                ("Female." + Clothes.class.getSimpleName()).toLowerCase()
                        + ".")) {
            name = name.substring(name.indexOf(
                    Clothes.class.getSimpleName().toLowerCase() + "."));
        } else if (("toys." + Toys.Ball_Gag.name()).equalsIgnoreCase(name)) {
            name = "toys.ballgag";
        }
        return name;
    }

    @Override
    public TextVariables getTextVariables(Locale locale) {
        TextVariables variables = new TextVariables();
        variables.put(TextVariables.Names.Slave,
                getLocalized("intro.name", locale));
        return variables;
    }

    private String getLocalized(String name, Locale locale) {
        if (defaultLanguageMatches(locale)) {
            return get(name);
        } else {
            return get(name + "." + locale);
        }
    }

    private static boolean defaultLanguageMatches(Locale locale) {
        Locale defaultLocale = Locale.getDefault();
        return defaultLocale.getLanguage().equals(locale.getLanguage());
    }

    @Override
    public Actor getDominant(Voice.Gender gender, Locale locale) {
        switch (gender) {
        case Female:
            return new Actor("Mistress", "Miss", Voice.Gender.Female, locale,
                    Actor.Key.DominantFemale, Images.None);
        case Male:
            return new Actor("Master", "Sir", Voice.Gender.Male, locale,
                    Actor.Key.DominantMale, Images.None);
        default:
            throw new IllegalArgumentException(gender.toString());
        }
    }
}
