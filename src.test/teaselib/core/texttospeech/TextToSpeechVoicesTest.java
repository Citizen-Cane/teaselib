package teaselib.core.texttospeech;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import teaselib.core.util.Environment;

@RunWith(Parameterized.class)
public class TextToSpeechVoicesTest {

    @ClassRule
    public static final TemporaryFolder testFolder = new TemporaryFolder();

    private static TextToSpeech textToSpeech;

    @AfterClass
    public static void cleanup() {
        try {
            textToSpeech.close();
        } finally {
            textToSpeech = null;
        }
    }

    @Parameters(name = "Voice ={0}")
    public static Iterable<Voice> voices() {
        Assume.assumeTrue(Environment.SYSTEM == Environment.Windows);
        assertNull("Resource not closed", textToSpeech);
        textToSpeech = TextToSpeech.allSystemVoices();
        Map<String, Voice> voices = textToSpeech.getVoices();
        assertTrue(voices.size() > 1);

        return voices.values();
    }

    private final Voice voice;

    public TextToSpeechVoicesTest(Voice voice) {
        this.voice = voice;
    }

    @Test
    public void testEachVoice() throws IOException {
        File testFile = testFolder.newFile(voice.guid() + ".wav");
        String file = textToSpeech.speak(voice, "Test.", testFile, new String[] {});
        assertEquals(testFile.getAbsolutePath(), file);
        assertTrue(testFile.exists());
    }

}
