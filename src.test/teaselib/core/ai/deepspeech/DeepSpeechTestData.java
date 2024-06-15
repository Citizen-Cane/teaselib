package teaselib.core.ai.deepspeech;

import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import teaselib.core.util.ReflectionUtils;

public class DeepSpeechTestData {

    static final DeepSpeechTestData AUDIO_2830_3980_0043_RAW = new DeepSpeechTestData(
            "2830-3980-0043.raw", "experience prooves this", "experience proves this");
    static final DeepSpeechTestData AUDIO_4507_16021_0012_RAW = new DeepSpeechTestData(
            "4507-16021-0012.raw","why should one halt on the way", "why should one halt on the way");
    static final DeepSpeechTestData AUDIO_8455_210777_0068_RAW = new DeepSpeechTestData(
            "8455-210777-0068.raw","your power is sufficient i said", "your part is sufficient i said");

    static final List<DeepSpeechTestData> tests = Arrays.asList( //
            AUDIO_2830_3980_0043_RAW, AUDIO_4507_16021_0012_RAW, AUDIO_8455_210777_0068_RAW);

    private final String audio;
    final String groundTruth;
    final String actual;


    public DeepSpeechTestData(String audio, String groundTruth, String actual) {
        this.audio = audio;
        this.groundTruth = groundTruth;
        this.actual = actual;
    }

    public Path audio() throws FileNotFoundException {
        return getPath(this.audio);
    }
    static Path getPath(String audio) throws FileNotFoundException {
        URL resource = DeepSpeechTestData.class.getResource(audio);
        if (resource == null) throw new FileNotFoundException();
        String path = ReflectionUtils.relativePath(resource.getPath());
        return Paths.get(path);
    }

    @Override
    public String toString() {
        return groundTruth;
    }

}
