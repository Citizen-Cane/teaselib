package teaselib.core.texttospeech;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextToSpeechInfrastructureTest {
    private static final Logger logger = LoggerFactory.getLogger(TextToSpeechInfrastructureTest.class);

    @Test
    public void testGetVoicesIsOrdered() {
        try (var tts1 = TextToSpeech.allSystemVoices(); var tts2 = TextToSpeech.allSystemVoices()) {
            var expected = tts1.getVoices();
            var actual = tts2.getVoices();
            assertEquals(expected.size(), actual.size());

            var ie = expected.entrySet().iterator();
            var ia = actual.entrySet().iterator();
            while (ie.hasNext() && ia.hasNext()) {
                var e = ie.next();
                var a = ia.next();
                assertEquals(e.getKey(), a.getKey());
            }
        }
    }

    @Test
    public void testTextToSpeechIsSessionInstance() {
        try (var tts1 = TextToSpeech.allSystemVoices(); var tts2 = TextToSpeech.allSystemVoices()) {
            var expected = tts1.getVoices();
            var actual = tts2.getVoices();
            assertEquals(expected.size(), actual.size());

            var ie = expected.entrySet().iterator();
            var ia = actual.entrySet().iterator();
            while (ie.hasNext() && ia.hasNext()) {
                var e = ie.next();
                var a = ia.next();
                assertEquals(e.getKey(), a.getKey());
                assertNotSame(
                        "Static bookkeeping in unstable because the native would be handled by multiple delegate threads",
                        e.getValue(), a.getValue());
                assertEquals(e.getValue(), a.getValue());
            }
            assertEquals(expected, actual);
        }
    }

    @Test
    public void testBlacklistedVoicesAreIgnored() {
        try (var tts1 = TextToSpeech.allSystemVoices()) {
            var voices = tts1.getVoices();
            assertFalse(voices.isEmpty());
            log(voices);
            for (Entry<String, Voice> voice : voices.entrySet()) {
                for (String blackListed : TextToSpeech.BlackList) {
                    assertNotEquals(voice.getKey(), blackListed);
                }
            }
        }
    }

    private static void log(Map<String, Voice> voices) {
        for (Entry<String, Voice> entry : voices.entrySet()) {
            logger.info(entry.getKey() + " = " + entry.getValue().toString());
        }
    }
}
