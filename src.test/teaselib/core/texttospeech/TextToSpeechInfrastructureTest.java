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

        assertEquals(new TextToSpeech().getVoices(), new TextToSpeech().getVoices());
    }

    @Test
    public void testVoiceEnumeration() {
        Assume.assumeTrue(Environment.SYSTEM == Environment.Windows);
        TextToSpeech textToSpeech = new TextToSpeech();

        Map<String, Voice> voices = textToSpeech.getVoices();
        assertTrue(voices.size() > 0);
        for (Entry<String, Voice> entry : voices.entrySet()) {
            logger.info(entry.getKey() + " = " + entry.getValue().toString());
        }
    }
}
