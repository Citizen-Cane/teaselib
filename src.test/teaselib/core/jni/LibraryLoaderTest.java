package teaselib.core.jni;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ai.TeaseLibAI;
import teaselib.core.ai.deepspeech.DeepSpeechRecognizer;
import teaselib.core.ai.perception.SceneCapture;
import teaselib.core.devices.xinput.XInputDevice;
import teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils;
import teaselib.core.speechrecognition.sapi.TeaseLibSRSimple;
import teaselib.core.texttospeech.TextToSpeechImplementation;
import teaselib.core.texttospeech.Voice;
import teaselib.core.texttospeech.implementation.TeaseLibTTS;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Intention;

public class LibraryLoaderTest {

    private static final Logger logger = LoggerFactory.getLogger(LibraryLoaderTest.class);

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testLoadAI() throws InterruptedException {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI();
                NativeObjectList<SceneCapture> devices = SceneCapture.devices()) {
            assertNotNull(devices);
            int n = 0;
            for (SceneCapture device : devices) {
                logger.info("Device {}: '{}' , enclosure location = {}", n++, device.name, device.location);
            }
        }

        try (var inputMethod = SpeechRecognitionTestUtils.getInputMethod(DeepSpeechRecognizer.class)) {
            Choices foobar = new Choices(Locale.ENGLISH, Intention.Decide, new Choice("Foo Bar"));
            SpeechRecognitionTestUtils.assertRecognized(inputMethod, foobar);
        }
    }

    @Test
    public void testLoadTTS() throws IOException {
        try (TextToSpeechImplementation tts = TeaseLibTTS.Microsoft.newInstance()) {
            List<Voice> voices = tts.getVoices();
            tts.setVoice(voices.get(0));
            File file = tempFolder.newFile();
            tts.speak("Foo bar", file.getAbsolutePath());
            assertTrue(Files.exists(file.toPath()));
        }
    }

    @Test
    public void testLoadSR() throws InterruptedException {
        try (var inputMethod = SpeechRecognitionTestUtils.getInputMethod(TeaseLibSRSimple.class)) {
            Choices foobar = new Choices(Locale.ENGLISH, Intention.Decide, new Choice("Foo Bar"));
            SpeechRecognitionTestUtils.assertRecognized(inputMethod, foobar);
        }
    }

    @Test
    public void testLoadXInput() {
        assertNotNull(XInputDevice.getDevicePaths());
    }

}
