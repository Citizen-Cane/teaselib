package teaselib.core.jni;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LibraryLoaderTest {

    private static final Logger logger = LoggerFactory.getLogger(LibraryLoaderTest.class);

    @TempDir
    Path tempFolder;

    @Test
    public void testLoadAI() throws InterruptedException {
        try (TeaseLibAI ignored = new TeaseLibAI();
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
            Path file = Files.createFile(tempFolder.resolve("FooBar.wav"));
            tts.speak("Foo bar", file.toAbsolutePath().toString());
            assertTrue(Files.exists(file));
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
