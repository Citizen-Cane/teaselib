package teaselib;

import teaselib.core.Images;
import teaselib.core.texttospeech.Voice;
import teaselib.util.SpeechRecognitionRejectedScript;

public class Actor {

    public final static String Dominant = "Dominant";

    /**
     * The name of the actor
     */
    public final String name;

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

    public Images images = null;
    public SpeechRecognitionRejectedScript speechRecognitionRejectedScript = null;

    public Actor(Actor actor) {
        this.name = actor.name;
        this.locale = actor.locale;
        this.language = actor.language;
        this.gender = actor.gender;
        this.key = actor.key;
        this.images = actor.images;
    }

    public Actor(String name, Voice.Gender gender, String locale) {
        this(name, gender, locale, null);
    }

    public Actor(String name, Voice.Gender gender, String locale,
            Images images) {
        super();
        this.name = name;
        this.locale = locale;
        this.language = language(locale);
        this.gender = gender;
        this.key = key();
        this.images = images;
    }

    private static String language(String locale) {
        return locale.substring(0, 2);
    }

    private String key() {
        return name + "." + gender + "." + language;
    }
}
