package teaselib;

public class Mood {
    public static String Prefix = "<attitude ";
    public static String Suffix = "/>";

    public static final boolean matches(String attitude) {
        return attitude.startsWith(Prefix) && attitude.endsWith(Suffix);
    }

    /**
     * The default mood, spoken with TTS default settings
     */
    public static final String Neutral = "<attitude neutral/>";

    /**
     * Mood when reading, concentrated, speaks somewhat slower, a bit louder
     */
    public static final String Reading = "<attitude reading/>";
}
