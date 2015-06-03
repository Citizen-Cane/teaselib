package teaselib;

public class Mood {
    public static String Prefix = "<mood=";
    public static String Suffix = ">";

    public static boolean isMood(String text) {
        return text.startsWith(Prefix) && text.endsWith(Suffix);
    }

    public static String extractName(String mood) {
        return mood.substring(Prefix.length(), mood.length() - Suffix.length());
    }

    /**
     * The default mood, spoken with TTS default settings
     */
    public static final String Neutral = "<mood=neutral>";

    /**
     * Mood when reading, concentrated, speaks somewhat slower, a bit louder
     */
    public static final String Reading = "<mood=reading>";

    public static final String Amused = "<mood=amused>";
    public static final String Angry = "<mood=angry>";
    public static final String Disappointed = "<mood=disappointed>";
    public static final String Friendly = "<mood=friendly>";
    public static final String Happy = "<mood=happy>";
    public static final String Harsh = "<mood=harsh>";
    public static final String Mean = "<mood=mean>";
    public static final String Pleased = "<mood=pleased>";
    public static final String Sceptic = "<mood=sceptic>";
    public static final String Smiling = "<mood=smiling>";
    public static final String Sorry = "<mood=sorry>";
    public static final String Strict = "<mood=strict>";
    public static final String Teasing = "<mood=tease>";
    public static final String Welcoming = "<mood=welcoming>";

    public static String lessIntense(String mood) {
        // todo find less intense matching mood
        return Neutral;
    }

    public static String moreIntense(String mood) {
        // todo find more intense matching mood
        return Neutral;
    }
}
