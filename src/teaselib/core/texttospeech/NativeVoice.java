package teaselib.core.texttospeech;

import teaselib.Sexuality.Gender;
import teaselib.core.jni.NativeObject;
import teaselib.core.texttospeech.implementation.TextToSpeechImplementationDebugProxy;

/**
 * @author Citizen-Cane
 * 
 */
public class NativeVoice extends NativeObject implements Voice {

    private final TextToSpeechImplementation ttsImpl;
    /**
     * A unique identifier for the voice. Only alphanumeric characters and dots are allowed. Avoid file system
     * characters like '/', '\', ':'.
     */
    private final String guid;
    private final Gender gender;
    private final String locale;

    private final VoiceInfo voiceInfo;

    public NativeVoice(long nativeObject, TextToSpeechImplementation ttsImpl, String guid, String locale, Gender gender,
            VoiceInfo voiceInfo) {
        super(nativeObject);
        this.ttsImpl = new TextToSpeechImplementationDebugProxy(ttsImpl);

        this.guid = guid;
        this.gender = gender;
        this.locale = locale;

        this.voiceInfo = voiceInfo;
    }

    @Override
    public TextToSpeechImplementation tts() {
        return ttsImpl;
    }

    @Override
    public String guid() {
        return guid;
    }

    @Override
    public Gender gender() {
        return gender;
    }

    @Override
    public String locale() {
        return locale;
    }

    @Override
    public VoiceInfo info() {
        return voiceInfo;
    }

    @Override
    protected native void dispose();

    @Override
    public String toString() {
        return TextToSpeechImplementation.toString(this);
    }
}
