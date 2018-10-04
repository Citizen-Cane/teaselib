package teaselib;

import java.util.Locale;

import teaselib.Sexuality.Gender;
import teaselib.util.SpeechRecognitionRejectedScript;
import teaselib.util.TextVariables;

public class Actor {
    public class Key {
        public final static String DominantFemale = "Dominant_Female";
        public final static String DominantMale = "Dominant_Male";
    }

    /**
     * How to address the actor.
     *
     */
    public enum FormOfAddress {
        /**
         * The actor's title or honorific, as Mistress/Master, Miss/Sir
         */
        Title,

        Miss,
        Mistress,
        Sir,
        Master,

        /**
         * The actor's short- or surname.
         */
        Name,
        /**
         * The actor's full or family name, prefixed by the title or honorific.
         */
        FullName
    }

    /**
     * The locale of the actor (e.g. "en-us", "en-uk", "de")
     */
    private final Locale locale;

    /**
     * The actor's gender.
     */
    public final Gender gender;

    /**
     * Unique key for persistence and matching voices.
     */
    public final String key;

    /**
     * Actor specific text variables. These don't contain neither the session text variables nor the defaults, so in
     * order to properly expand a message call {@link teaselib.core.Script#expandTextVariables}.
     */
    public final TextVariables textVariables;

    /**
     * The actor's images.
     */
    public Images images = Images.None;

    /**
     * Optional script to execute when speech is not recognized.
     */
    public SpeechRecognitionRejectedScript speechRecognitionRejectedScript = null;

    public Actor(Actor actor) {
        this(actor, actor.locale());
    }

    public Actor(Actor actor, Locale locale) {
        this.locale = locale;
        this.gender = actor.gender;
        this.key = key(actor, locale);
        this.textVariables = actor.textVariables;
        this.images = actor.images;
    }

    public Actor(String fullName, Gender gender, Locale locale) {
        this(fullName, gender, locale, Images.None);
    }

    public Actor(String fullName, String title, Gender gender, Locale locale) {
        this(fullName, title, title, gender, locale, key(fullName), Images.None);
    }

    public Actor(String fullName, Gender gender, Locale locale, Images images) {
        this(fullName, fullName, fullName, gender, locale, key(fullName), images);
    }

    public Actor(String fullName, String title, Gender gender, Locale locale, String key, Images images) {
        this(fullName, title, title, gender, locale, key, images);
    }

    public Actor(String fullName, String title, String name, Gender gender, Locale locale, Images images) {
        this(fullName, title, name, gender, locale, key(fullName), images);
    }

    public Actor(String fullName, String title, String name, Gender gender, Locale locale, String key, Images images) {
        super();
        this.locale = locale;
        this.gender = gender;
        this.textVariables = new TextVariables();
        addTextVariables(title, fullName, name);
        this.key = escape(key);
        this.images = images;
    }

    private void addTextVariables(String title, String fullName, String name) {
        textVariables.put(FormOfAddress.Title, title);
        textVariables.put(FormOfAddress.Miss, title);
        textVariables.put(FormOfAddress.Sir, title);
        textVariables.put(FormOfAddress.Mistress, title);
        textVariables.put(FormOfAddress.Master, title);
        textVariables.put(FormOfAddress.FullName, fullName);
        textVariables.put(FormOfAddress.Name, name);
    }

    private static String key(String fullName) {
        return escape(fullName);
    }

    private static String key(Actor actor, Locale locale) {
        if (actor.locale().getCountry().equals(locale.getCountry())) {
            return key(actor.get(FormOfAddress.FullName));
        }
        return key(actor.get(FormOfAddress.FullName)) + "_" + locale;
    }

    private static String escape(String string) {
        return string.replace(" ", "_");
    }

    public String get(Enum<?> formOfAddress) {
        return textVariables.get(formOfAddress);
    }

    @Override
    public String toString() {
        return "'" + get(FormOfAddress.FullName) + "(" + gender + "," + locale() + ")' key=" + key;
    }

    public Locale locale() {
        return locale;
    }
}
