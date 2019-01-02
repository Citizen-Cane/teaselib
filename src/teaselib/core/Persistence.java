package teaselib.core;

import java.io.IOException;
import java.util.Locale;

import teaselib.Actor;
import teaselib.Sexuality.Gender;
import teaselib.core.util.QualifiedName;
import teaselib.util.TextVariables;

public interface Persistence {

    UserItems getUserItems(TeaseLib teaseLib) throws IOException;

    boolean has(QualifiedName name);

    /**
     * @param name
     *            The name of the property
     * @return The value of the property or null if not found
     */
    String get(QualifiedName name);

    void set(QualifiedName name, String value);

    boolean getBoolean(QualifiedName name);

    void set(QualifiedName name, boolean value);

    void clear(QualifiedName name);

    /**
     * Text variables may depend on the locale of the script. For instance, if the name of the slave can't be pronounced
     * correctly in a language, the host might return a different slave name for that language.
     * 
     * @param locale
     * @return
     */
    TextVariables getTextVariables(Locale locale);

    public Actor getDominant(Gender gender, Locale locale);
}
