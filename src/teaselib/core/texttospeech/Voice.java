package teaselib.core.texttospeech;

import java.util.Locale;

import teaselib.core.jni.NativeObject;
import teaselib.core.texttospeech.implementation.TextToSpeechImplementationDebugProxy;

/**
 * @author Citizen-Cane
 * 
 */
public class Voice extends NativeObject {
    final TextToSpeechImplementation ttsImpl;
    /**
     * A unique identifier for the voice. Only alphanumeric characters and dots are allowed. Avoid file system
     * characters like '/', '\', ':'.
     */
    public final String guid;
    public final Gender gender;
    public final String locale;
    public final String language;
    public final String name;
    public final String vendor;

    public enum Gender {
        Male,
        Female,
        Robot
    }

    public Voice(long nativeObject, TextToSpeechImplementation ttsImpl, String guid, String locale, Gender gender,
            VoiceInfo voiceInfo) {
        super(nativeObject);
        this.ttsImpl = new TextToSpeechImplementationDebugProxy(ttsImpl);

        this.guid = guid;
        this.gender = gender;
        this.locale = locale;
        this.language = voiceInfo.language;
        this.name = voiceInfo.name;
        this.vendor = voiceInfo.vendor;
    }

    public boolean matches(Locale locale) {
        return this.locale.replace("-", "_").equalsIgnoreCase(locale.toString());
    }

    @Override
    public String toString() {
        return "[guid=" + guid + ", gender= " + gender + ", locale=" + locale + ", language=" + language + ", name="
                + name + ", vendor=" + vendor + ", sdk=" + ttsImpl.sdkName() + "]";
    }
}
