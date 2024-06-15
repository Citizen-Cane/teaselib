package teaselib.core.texttospeech;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import teaselib.core.util.Environment;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TextToSpeechVoicesTest {

    @TempDir
    Path testFolder;

    private static TextToSpeech textToSpeech;

    @BeforeAll
    public static void init() {
        assertNull(textToSpeech, "Resource not closed");
        textToSpeech = TextToSpeech.allSystemVoices();
    }

        @AfterAll
    public static void cleanup() {
        try {
            textToSpeech.close();
        } finally {
            textToSpeech = null;
        }
    }

    public static Iterable<Voice> voices() {
        Assumptions.assumeTrue(Environment.SYSTEM == Environment.Windows);
        Map<String, Voice> voices = textToSpeech.getVoices();
        assertTrue(voices.size() > 1);

        return voices.values();
    }

    @ParameterizedTest
    @MethodSource("voices")
    public void testEachVoice(Voice voice) throws IOException {
        File testFile = Files.createFile(testFolder.resolve(voice.guid() + ".wav")).toFile();
        String file = textToSpeech.speak(voice, "Test.", testFile, new String[] {});
        Assertions.assertEquals(testFile.getAbsolutePath(), file);
        assertTrue(testFile.exists());
    }

}
