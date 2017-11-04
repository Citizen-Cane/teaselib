package teaselib.core.texttospeech.implementation.loquendo;

import teaselib.Sexuality.Gender;
import teaselib.core.texttospeech.TextToSpeechImplementation;
import teaselib.core.texttospeech.Voice;
import teaselib.core.texttospeech.VoiceInfo;
import teaselib.core.texttospeech.implementation.loquendo.LoquendoTTSLibrary.ttsHandleType;

public class LoquendoVoice implements Voice {
    private final LoquendoTTS tts;

    final ttsHandleType hVoice;

    public LoquendoVoice(LoquendoTTS tts, ttsHandleType hVoice) {
        this.tts = tts;
        this.hVoice = hVoice;
    }

    @Override
    public TextToSpeechImplementation tts() {
        // TODO Auto-generated method stub
        return tts;
    }

    @Override
    public String guid() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Gender gender() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String locale() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VoiceInfo info() {
        // TODO Auto-generated method stub
        return null;
    }

}
