package teaselib.core.texttospeech;

import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.util.Environment;

// TODO on Windows fails to cleanup and
// crashes the test suite while executing a subsequent test
public class TextToSpeechTest {
    private static final Logger logger = LoggerFactory.getLogger(TextToSpeechTest.class);

    @Test
    public void testVoiceEnumeration() {
        Assume.assumeTrue(Environment.SYSTEM == Environment.Windows);
        TextToSpeech textToSpeech = new TextToSpeech();
        assertTrue(textToSpeech.isReady());
        Map<String, Voice> voices = textToSpeech.getVoices();
        assertTrue(voices.size() > 0);
        for (Entry<String, Voice> entry : voices.entrySet()) {
            logger.info(entry.getKey() + " = " + entry.getValue().toString());
        }
    }

    @Test
    @Ignore
    public void testEachVoice() throws InterruptedException {
        Assume.assumeTrue(Environment.SYSTEM == Environment.Windows);
        TextToSpeech textToSpeech = new TextToSpeech();
        assertTrue(textToSpeech.isReady());
        Map<String, Voice> voices = textToSpeech.getVoices();
        assertTrue(voices.size() > 0);
        for (Entry<String, Voice> entry : voices.entrySet()) {
            String testPrompt = "Test.";
            logger.info("Testing voice " + entry.getKey() + " - prompt =  '" + testPrompt + "'");
            textToSpeech.speak(entry.getValue(), testPrompt);
        }
    }
}
