package teaselib;

import teaselib.core.texttospeech.Voice;
import teaselib.core.texttospeech.Voice.Gender;
import teaselib.util.SpeechRecognitionRejectedScript;
import teaselib.util.TextVariables;

public class Actor {
    public class Key {
        public final static String Dominant = "Dominant";
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
         * The actor's surname.
         */
        Name,
        /**
         * The actor's full or family name, prefixed by the title or honorific.
         */
        FullName
    }

    /**
     * The locale of the actor (e.g. "en-us", "en-gb")
     */
    public final String locale;

    /**
     * The actor's language (e.g. "en", "de"), derived from the locale
     */
    public final String language;

    /**
     * The actor's gender.
     */
    public final Voice.Gender gender;

    /**
     * Unique key for persistence and matching voices.
     */
    public final String key;

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
        this.locale = actor.locale;
        this.language = actor.language;
        this.gender = actor.gender;
        this.key = actor.key;
        this.textVariables = actor.textVariables;
        this.images = actor.images;
    }

    public Actor(String fullName, Voice.Gender gender, String locale) {
        this(fullName, gender, locale, Images.None);
    }

    public Actor(String fullName, String title, Voice.Gender gender,
            String locale) {
        this(fullName, title, title, gender, locale,
                key(fullName, gender, locale), Images.None);
    }

    public Actor(String fullName, Voice.Gender gender, String locale,
            Images images) {
        this(fullName, fullName, fullName, gender, locale,
                key(fullName, gender, locale), images);
    }

    public Actor(String fullName, String title, Voice.Gender gender,
            String locale, String key, Images images) {
        this(fullName, title, title, gender, locale, key, images);
    }

    public Actor(String fullName, String title, String name,
            Voice.Gender gender, String locale, Images images) {
        this(fullName, title, name, gender, locale,
                key(fullName, gender, locale), images);
    }

    public Actor(String fullName, String title, String name,
            Voice.Gender gender, String locale, String key, Images images) {
        super();
        this.locale = locale;
        this.language = language(locale);
        this.gender = gender;
        this.textVariables = new TextVariables();
        addTextVariables(title, fullName, name);
        this.key = key;
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

    private static String language(String locale) {
        return locale.substring(0, 2);
    }

    private static String key(String fullName, Gender gender, String language) {
        return fullName + "." + gender + "." + language;
    }

    public String get(Enum<?> formOfAddress) {
        return textVariables.get(formOfAddress);
    }

    @Override
    public String toString() {
        return get(FormOfAddress.FullName) + ": " + locale + " " + gender;
    }

}
