package teaselib.core.texttospeech;

import teaselib.Sexuality.Gender;

/**
 * @author Citizen-Cane
 *
 */
public interface Voice {
    static final Gender Male = Gender.Masculine;
    static final Gender Female = Gender.Feminine;

    TextToSpeechImplementation tts();

    String guid();

    Gender gender();

    String locale();

    VoiceInfo info();
}
