package teaselib.core.ai.deepspeech;

import static org.junit.jupiter.api.Assertions.fail;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.Test;

import teaselib.core.ai.TeaseLibAI;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.util.ReflectionUtils;

public class DeepSpeechRecognizerTest {

    private static final Path project = ReflectionUtils.projectPath(DeepSpeechRecognizer.class);
    private static final Path model = project.resolve(Path.of( //
            "..", "..", "TeaseLibAIfx", "TeaseLibAIml", "models", "tflite", "deepspeech")).normalize();
    private static final Path AUDIO_2830_3980_0043_RAW = model.resolve(Path.of("audio/2830-3980-0043.raw"));
    private static final Path AUDIO_4507_16021_0012_RAW = model.resolve(Path.of("audio/4507-16021-0012.raw"));
    private static final Path AUDIO_8455_210777_0068_RAW = model.resolve(Path.of("audio/8455-210777-0068.raw"));

    @Test
    public void testExperienceProovesThis() throws URISyntaxException {
        testAudio(AUDIO_2830_3980_0043_RAW);
    }

    @Test
    public void testWhyShouldOneHaltOnTheWay() throws URISyntaxException {
        testAudio(AUDIO_4507_16021_0012_RAW);
    }

    @Test
    public void testYourPowerIsSufficientIsaid() throws URISyntaxException {
        testAudio(AUDIO_8455_210777_0068_RAW);
    }

    public void testAudio(Path audioFile) throws URISyntaxException {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            SpeechRecognitionEvents events = new SpeechRecognitionEvents();
            try (DeepSpeechRecognizer deepSpeechRecognizer = new DeepSpeechRecognizer(model, Locale.ENGLISH, events)) {
                deepSpeechRecognizer.emulateRecognition(audioFile.toString());
            }
        }
    }

    @Test
    public void testEmulateText() {
        fail("TODO");
    }

}
