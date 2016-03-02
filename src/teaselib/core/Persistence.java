package teaselib.core;

import teaselib.Actor;
import teaselib.core.texttospeech.Voice;
import teaselib.util.TextVariables;

public interface Persistence {

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
    TextVariables getTextVariables(String locale);

    public Actor getDominant(String locale);

    public Actor getDominant(Voice.Gender gender, String locale);
}
