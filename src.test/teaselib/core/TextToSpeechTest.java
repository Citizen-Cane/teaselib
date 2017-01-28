package teaselib.core;

import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.texttospeech.TextToSpeech;
import teaselib.core.texttospeech.Voice;
import teaselib.core.util.Environment;

// TODO Fails to cleanup on Windows somehow and
// crashes the test suite after finishing successfully
@Ignore
public class TextToSpeechTest {
    private static final Logger logger = LoggerFactory
            .getLogger(TextToSpeechTest.class);

    @Test
    @Ignore
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
    public void testEachVoice() throws InterruptedException {
        Assume.assumeTrue(Environment.SYSTEM == Environment.Windows);
        TextToSpeech textToSpeech = new TextToSpeech();
        assertTrue(textToSpeech.isReady());
        Map<String, Voice> voices = textToSpeech.getVoices();
        assertTrue(voices.size() > 0);
        for (Entry<String, Voice> entry : voices.entrySet()) {
            String testPrompt = "Test.";
            logger.info("Testing voice " + entry.getKey() + " - prompt =  '"
                    + testPrompt + "'");
            textToSpeech.setVoice(entry.getValue());
            textToSpeech.speak(testPrompt);
        }
    }
}
