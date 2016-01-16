package teaselib;

import teaselib.core.Images;
import teaselib.util.SpeechRecognitionRejectedScript;

public class Actor {

    public final static String Dominant = "Dominant";

    public final String name;
    public final String locale;
    public final String language;
    public final String key;

    public Images images = null;
    public SpeechRecognitionRejectedScript speechRecognitionRejectedScript = null;

    public Actor(Actor actor) {
        this.name = actor.name;
        this.locale = actor.locale;
        this.language = actor.language;
        this.key = actor.key;
        this.images = actor.images;
    }

    public Actor(String name, String locale) {
        this(name, locale, null);
    }

    public Actor(String name, String locale, Images images) {
        super();
        this.name = name;
        this.locale = locale;
        this.language = language(locale);
        this.key = key();
        this.images = images;
    }

    private static String language(String locale) {
        return locale.substring(0, 2);
    }

    private String key() {
        return name + "." + language;
    }
}
