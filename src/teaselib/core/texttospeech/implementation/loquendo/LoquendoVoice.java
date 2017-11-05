package teaselib.core.texttospeech.implementation.loquendo;

import org.bridj.Pointer;

import teaselib.Sexuality.Gender;
import teaselib.core.texttospeech.TextToSpeechImplementation;
import teaselib.core.texttospeech.Voice;
import teaselib.core.texttospeech.VoiceInfo;

public class LoquendoVoice implements Voice {
    private final LoquendoTTS tts;
    final Pointer<?> hVoice;
    final Pointer<?> hLanguage;

    private final String guid;
    private final Gender gender;
    private final String locale;

    private final VoiceInfo info;

    public LoquendoVoice(LoquendoTTS tts, Pointer<?> hVoice, Pointer<?> hLanguage, String id) {
        this.tts = tts;
        this.hVoice = hVoice;
        this.hLanguage = hLanguage;

        this.guid = tts.sdkName() + "_" + id;
        this.gender = gender(tts.queryVoiceAttribute(Pointer.pointerToCString(id), "Gender"));
        String language = tts.queryVoiceAttribute(Pointer.pointerToCString(id), "MotherTongue");

        // TODO Extract locale ??-??
        this.locale = tts.queryLanguageAttribute(Pointer.pointerToCString(language), "Language");

        this.info = new VoiceInfo("Loquendo",
                // TODO remove "language" from description
                tts.queryLanguageAttribute(Pointer.pointerToCString(language), "Description"), id);
    }

    private static Gender gender(String gender) {
        if ("male".equalsIgnoreCase(gender)) {
            return Voice.Male;
        } else if ("female".equalsIgnoreCase(gender)) {
            return Voice.Female;
        } else {
            throw new IllegalArgumentException();
        }

    }

    @Override
    public TextToSpeechImplementation tts() {
        return tts;
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
        return info;
    }

    @Override
    public String toString() {
        return TextToSpeechImplementation.toString(this);
    }
}
