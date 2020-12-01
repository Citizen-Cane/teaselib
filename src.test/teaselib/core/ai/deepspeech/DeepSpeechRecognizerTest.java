package teaselib.core.ai.deepspeech;

import static org.junit.jupiter.api.Assertions.fail;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import org.junit.Test;

import teaselib.core.ai.TeaseLibAI;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;

public class DeepSpeechRecognizerTest {

    private static final String AUDIO_2830_3980_0043_RAW = "audio/2830-3980-0043.raw";
    private static final String AUDIO_4507_16021_0012_RAW = "audio/4507-16021-0012.raw";
    private static final String AUDIO_8455_210777_0068_RAW = "audio/8455-210777-0068.raw";

    @Test
    public void testEmulateAudioFile() throws URISyntaxException {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            Path path = Paths.get(getClass().getResource(AUDIO_2830_3980_0043_RAW).toURI());
            SpeechRecognitionEvents events = new SpeechRecognitionEvents();
            try (DeepSpeechRecognizer deepSpeechRecognizer = new DeepSpeechRecognizer(Locale.ENGLISH, events)) {
                deepSpeechRecognizer.emulateRecognition(path.toString());
            }
        }
    }

    @Test
    public void testEmulateText() {
        fail("TODO");
    }

}
