package teaselib.core.ai.deepspeech;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import teaselib.core.util.ReflectionUtils;

public class DeepSpeechTestData {

    private static final Path projectPath = ReflectionUtils.projectPath(DeepSpeechTestData.class);
    static final Path testDataPath = projectPath.resolve(Path.of( //
            "..", "..", "TeaseLibAIfx", "AIml", "models", "tflite", "deepspeech")).normalize();

    static final DeepSpeechTestData AUDIO_2830_3980_0043_RAW = new DeepSpeechTestData(
            testDataPath.resolve(Path.of(Locale.ENGLISH.getLanguage() + "-audio/2830-3980-0043.raw")), //
            "experience prooves this", "experience proves this");
    static final DeepSpeechTestData AUDIO_4507_16021_0012_RAW = new DeepSpeechTestData(
            testDataPath.resolve(Path.of(Locale.ENGLISH.getLanguage() + "-audio/4507-16021-0012.raw")), //
            "why should one halt on the way", "why should one halt on the way");
    static final DeepSpeechTestData AUDIO_8455_210777_0068_RAW = new DeepSpeechTestData(
            testDataPath.resolve(Path.of(Locale.ENGLISH.getLanguage() + "-audio/8455-210777-0068.raw")), //
            "your power is sufficient i said", "your part is sufficient i said");

    static final List<DeepSpeechTestData> tests = Arrays.asList( //
            AUDIO_2830_3980_0043_RAW, AUDIO_4507_16021_0012_RAW, AUDIO_8455_210777_0068_RAW);

    final Path audio;
    final String groundTruth;
    final String actual;

    public DeepSpeechTestData(Path audio, String groundTruth, String expected) {
        this.audio = audio;
        this.groundTruth = groundTruth;
        this.actual = expected;
    }

    @Override
    public String toString() {
        return groundTruth;
    }

}
