package teaselib.core.texttospeech;

import teaselib.core.jni.NativeObject;

// refactor -> change class name in native code

/**
 * @author someone
 *
 */
/**
 * @author someone
 * 
 */
public class Voice extends NativeObject {
    /**
     * A unique identifier for the voice. Only alphanumeric characters and dots
     * are allowed. Avoid file system characters like '/', '\', ':'.
     */
    public final String guid;
    public final String locale;
    public final String language;
    public final Gender gender;
    public final String name;
    public final String vendor;

    public enum Gender {
        Male, Female, Robot
    }

    public Voice(long nativeObject, String guid, String locale,
            String language, Gender gender, String name, String vendor) {
        super(nativeObject);
        this.guid = guid;
        this.gender = gender;
        this.locale = locale;
        this.language = language;
        this.name = name;
        this.vendor = vendor;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": guid=" + guid + " , gender= "
                + gender + " , locale=" + locale + " , language=" + language
                + " , name=" + name + " , vendor=" + vendor;
    }
}
