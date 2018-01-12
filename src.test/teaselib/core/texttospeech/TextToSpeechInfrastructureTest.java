package teaselib.core.texttospeech;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.util.Environment;

public class TextToSpeechInfrastructureTest {
    private static final Logger logger = LoggerFactory.getLogger(TextToSpeechInfrastructureTest.class);

    @Test
    public void testGetVoicesIsOrdered() {
        Assume.assumeTrue(Environment.SYSTEM == Environment.Windows);

        Map<String, Voice> actual = new TextToSpeech().getVoices();
        Map<String, Voice> expected = new TextToSpeech().getVoices();
        assertEquals(expected, actual);
    }

    @Test
    public void testBlacklistedVoicesAreIgnored() {
        Assume.assumeTrue(Environment.SYSTEM == Environment.Windows);

        Map<String, Voice> voices = new TextToSpeech().getVoices();
        for (Entry<String, Voice> voice : voices.entrySet()) {
            for (String blackListed : TextToSpeech.BlackList) {
                assertFalse(voice.getKey().equals(blackListed));
            }
        }
    }

    @Test
    public void testVoiceEnumeration() throws InterruptedException {
        Assume.assumeTrue(Environment.SYSTEM == Environment.Windows);
        TextToSpeech textToSpeech = new TextToSpeech();
        enumVoices(textToSpeech);

        Thread thread = new Thread(() -> {
            TextToSpeech textToSpeech2 = new TextToSpeech();
            enumVoices(textToSpeech2);
        });
        thread.start();
        thread.join();

        TextToSpeech textToSpeech3 = new TextToSpeech();
        enumVoices(textToSpeech3);
    }

    private static void enumVoices(TextToSpeech textToSpeech) {
        Map<String, Voice> voices = textToSpeech.getVoices();
        assertTrue(voices.size() > 0);
        for (Entry<String, Voice> entry : voices.entrySet()) {
            logger.info(entry.getKey() + " = " + entry.getValue().toString());
        }
    }
}
