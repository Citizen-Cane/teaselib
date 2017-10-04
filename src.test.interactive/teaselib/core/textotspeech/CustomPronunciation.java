package teaselib.core.textotspeech;

import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.texttospeech.TextToSpeech;
import teaselib.core.texttospeech.TextToSpeechTest;
import teaselib.core.texttospeech.Voice;
import teaselib.core.util.Environment;

public class CustomPronunciation {

    private static final Logger logger = LoggerFactory.getLogger(TextToSpeechTest.class);

    // Cum with "u" -> wrong
    // Cunt right
    static final String MS_VOICE = "TTS_MS_en-US_ZiraPro_11.0";

    // Cum = come -> right
    // Cunt with "u" -> wrong
    static final String LQ_VOICE = "LTTS7Allison";

    // static final String PROMPT0 = "cunt!";
    // static final String PROMPT0 = "<emph>cunt</emph>!";

    // IPA pronunciation ignored
    // static final String PROMPT1 = "<P DISP=\"cunt\" PRON=\"H EH 1 L OW & W ER 1 L D\">cunt</P>cunt";
    static final String PROMPT1 = "<P DISP=\"cunt\" PRON=\"h eh 1 l ow\">cunt</P>cunt";

    // COM-Error
    // TODO <emph> works, it's the same XML TTS tutorial
    // static final String PROMPT2 = "<pron sym=\"H EH 1 L OW & W ER 1 L D\"> hello world </pron>";

    // Without the "&", the pron tag is just ignored (no speech output)
    // -> Works with MS voice, ignored by Loquendo -> must use Loquendo pron tags ...
    static final String PROMPT2 = "<pron sym=\"H EH 1 L OW \"> hello </pron>";

    String prompt = PROMPT1;
    String voiceGuid = MS_VOICE;

    @Test
    public void testCustomPronunciation() throws InterruptedException {
        Assume.assumeTrue(Environment.SYSTEM == Environment.Windows);
        TextToSpeech textToSpeech = new TextToSpeech();
        assertTrue(textToSpeech.isReady());
        Map<String, Voice> voices = textToSpeech.getVoices();
        assertTrue(voices.size() > 0);
        logger.info(voices.keySet().toString());

        Voice voice = voices.get(voiceGuid);
        logger.info(voice + " -> " + prompt);

        textToSpeech.setVoice(voice);
        textToSpeech.speak(prompt);
    }

}
