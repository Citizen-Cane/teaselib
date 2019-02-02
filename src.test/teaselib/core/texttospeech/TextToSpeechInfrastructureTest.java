package teaselib.core.texttospeech;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

        Map<String, Voice> actual = TextToSpeech.allSystemVoices().getVoices();
        Map<String, Voice> expected = TextToSpeech.allSystemVoices().getVoices();
        assertEquals(expected, actual);
    }

    @Test
    public void testBlacklistedVoicesAreIgnored() {
        Assume.assumeTrue(Environment.SYSTEM == Environment.Windows);

        Map<String, Voice> voices = TextToSpeech.allSystemVoices().getVoices();
        for (Entry<String, Voice> voice : voices.entrySet()) {
            for (String blackListed : TextToSpeech.BlackList) {
                assertFalse(voice.getKey().equals(blackListed));
            }
        }
    }

    @Test
    public void testVoiceEnumeration() throws InterruptedException {
        Assume.assumeTrue(Environment.SYSTEM == Environment.Windows);
        TextToSpeech textToSpeech = TextToSpeech.allSystemVoices();
        enumVoices(textToSpeech);

        Thread thread = new Thread(() -> {
            TextToSpeech textToSpeech2 = TextToSpeech.allSystemVoices();
            enumVoices(textToSpeech2);
        });
        thread.start();
        thread.join();

        TextToSpeech textToSpeech3 = TextToSpeech.allSystemVoices();
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
