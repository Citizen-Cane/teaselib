package teaselib;

public class Mood {
    public static String Prefix = "<mood=";
    public static String Suffix = ">";

    public static final boolean isMood(String text) {
        return text.startsWith(Prefix) && text.endsWith(Suffix);
    }

    /**
     * The default mood, spoken with TTS default settings
     */
    public static final String Neutral = "mood=neutral";

    /**
     * Mood when reading, concentrated, speaks somewhat slower, a bit louder
     */
    public static final String Reading = "mood=reading";
}
