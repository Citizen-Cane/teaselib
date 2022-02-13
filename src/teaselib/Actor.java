package teaselib;

import java.util.Locale;

import teaselib.Sexuality.Gender;
import teaselib.util.SpeechRecognitionRejectedScript;
import teaselib.util.TextVariables;
import teaselib.util.TextVariables.FormOfAddress;

public class Actor {
    public class Key {
        private Key() {
        }

        public static final String DominantFemale = "Dominant_Female";
        public static final String DominantMale = "Dominant_Male";
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
    // TODO Instead of image cache instances, use final path string and create caches internally
    // -> static final initialization
    public ActorImages images = ActorImages.None;

    /**
     * This actor's instructional images.
     */
    public ImageCollection instructions = ImageCollection.None;

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
        this(fullName, gender, locale, ActorImages.None);
    }

    public Actor(String fullName, String title, Gender gender, Locale locale) {
        this(fullName, title, title, gender, locale, key(fullName), ActorImages.None);
    }

    public Actor(String fullName, Gender gender, Locale locale, ActorImages images) {
        this(fullName, fullName, fullName, gender, locale, key(fullName), images);
    }

    public Actor(String fullName, String title, Gender gender, Locale locale, String key, ActorImages images) {
        this(fullName, title, title, gender, locale, key, images);
    }

    public Actor(String fullName, String title, String name, Gender gender, Locale locale, ActorImages images) {
        this(fullName, title, name, gender, locale, key(fullName), images);
    }

    public Actor(String fullName, String title, String name, Gender gender, Locale locale, String key,
            ActorImages images) {
        super();
        this.locale = locale;
        this.gender = gender;
        this.textVariables = new TextVariables();
        addTextVariables(title, fullName, name);
        this.key = escape(key);
        this.images = images;
    }

    private void addTextVariables(String title, String fullName, String name) {
        textVariables.set(FormOfAddress.Title, title);
        textVariables.set(FormOfAddress.FullName, fullName);
        textVariables.set(FormOfAddress.Name, name);
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
        return textVariables.get(formOfAddress.name());
    }

    @Override
    public String toString() {
        return "'" + get(FormOfAddress.FullName) + "(" + gender + "," + locale() + ")' key=" + key;
    }

    public Locale locale() {
        return locale;
    }
}
