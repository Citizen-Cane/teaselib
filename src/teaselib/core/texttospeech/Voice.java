package teaselib.core.texttospeech;

import java.util.Locale;

import teaselib.core.jni.NativeObject;

/**
 * @author someone
 * 
 */
public class Voice extends NativeObject {
    /**
     * A unique identifier for the voice. Only alphanumeric characters and dots are allowed. Avoid file system
     * characters like '/', '\', ':'.
     */
    public final String guid;
    public final String locale;
    public final String language;
    public final Gender gender;
    public final String name;
    public final String vendor;
    public final String api;

    public enum Gender {
        Male,
        Female,
        Robot
    }

    public Voice(long nativeObject, String guid, String locale, String language, Gender gender, String name,
            String vendor, String api) {
        super(nativeObject);
        this.guid = guid;
        this.gender = gender;
        this.locale = locale;
        this.language = language;
        this.name = name;
        this.vendor = vendor;
        this.api = api;
    }

    public boolean matches(Locale locale) {
        return this.locale.replace("-", "_").equalsIgnoreCase(locale.toString());
    }

    @Override
    public String toString() {
        return "[guid=" + guid + ", gender= " + gender + ", locale=" + locale + ", language=" + language + ", name="
                + name + ", vendor=" + vendor + ", api=" + api + "]";
    }
}
