package teaselib.core.ai.deepspeech;

import java.nio.file.Path;
import java.util.Locale;

public class DeepSpeechTestData {

    static final DeepSpeechTestData AUDIO_2830_3980_0043_RAW = new DeepSpeechTestData(
            DeepSpeechRecognizer.modelPath.resolve(Path.of(Locale.ENGLISH.getLanguage(), "audio/2830-3980-0043.raw")), //
            "experience prooves this", "experience proves this");
    static final DeepSpeechTestData AUDIO_4507_16021_0012_RAW = new DeepSpeechTestData(
            DeepSpeechRecognizer.modelPath.resolve(Path.of(Locale.ENGLISH.getLanguage(), "audio/4507-16021-0012.raw")), //
            "why should one halt on the way", "why should one halt on the way");
    static final DeepSpeechTestData AUDIO_8455_210777_0068_RAW = new DeepSpeechTestData(
            DeepSpeechRecognizer.modelPath.resolve(Path.of(Locale.ENGLISH.getLanguage(), "audio/8455-210777-0068.raw")), //
            "your power is sufficient i said", "your part is sufficient i said");

    final Path audio;
    final String expected;
    final String actual;

    public DeepSpeechTestData(Path audio, String groundThruth, String expected) {
        this.audio = audio;
        this.expected = groundThruth;
        this.actual = expected;
    }

}
