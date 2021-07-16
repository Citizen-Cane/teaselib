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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((gender == null) ? 0 : gender.hashCode());
        result = prime * result + ((guid == null) ? 0 : guid.hashCode());
        result = prime * result + ((locale == null) ? 0 : locale.hashCode());
        result = prime * result + ((ttsImpl == null) ? 0 : ttsImpl.hashCode());
        result = prime * result + ((voiceInfo == null) ? 0 : voiceInfo.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NativeVoice other = (NativeVoice) obj;
        if (gender != other.gender)
            return false;
        if (guid == null) {
            if (other.guid != null)
                return false;
        } else if (!guid.equals(other.guid))
            return false;
        if (locale == null) {
            if (other.locale != null)
                return false;
        } else if (!locale.equals(other.locale))
            return false;
        if (ttsImpl == null) {
            if (other.ttsImpl != null)
                return false;
        } else if (!ttsImpl.sdkName().equals(other.ttsImpl.sdkName()))
            return false;
        if (voiceInfo == null) {
            if (other.voiceInfo != null)
                return false;
        } else if (!voiceInfo.equals(other.voiceInfo))
            return false;
        return true;
    }

}
