package teaselib.core;

import java.util.Locale;

import teaselib.Actor;
import teaselib.core.texttospeech.Voice;
import teaselib.core.util.PropertyNameMapping;
import teaselib.util.TextVariables;

public interface Persistence {

    PropertyNameMapping getNameMapping();

    UserItems getUserItems();

    boolean has(String name);

    /**
     * @param name
     *            The name of the property
     * @return The value of the property or null if not found
     */
    String get(String name);

    void set(String name, String value);

    boolean getBoolean(String name);

    void set(String name, boolean value);

    void clear(String name);

    /**
     * Text variables may depend on the locale of the script. For instance, if
     * the name of the slave can't be pronounced correctly in a language, the
     * host might return a different slave name for that language.
     * 
     * @param locale
     * @return
     */
    TextVariables getTextVariables(Locale locale);

    public Actor getDominant(Voice.Gender gender, Locale locale);
}
