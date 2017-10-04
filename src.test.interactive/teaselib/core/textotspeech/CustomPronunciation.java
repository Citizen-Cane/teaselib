package teaselib.core.textotspeech;

import static org.junit.Assert.*;

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

    static final String PROMPT0a = "cunt!";
    static final String PROMPT0b = "<emph>cunt</emph>!";

    // IPA pronunciation ignored
    static final String PROMPT1a = "<P DISP=\"cunt\" PRON=\"H EH 1 L OW & W ER 1 L D\">cunt</P>cunt";
    static final String PROMPT1b = "<P DISP=\"cunt\" PRON=\"h eh 1 l ow\">cunt</P>cunt";
    // not recognized
    static final String PROMPT1c = "<P>/cunt/cunt/H EH 1 L OW;</P>";

    // COM-Error because of the &
    // TODO <emph> works, it's the same XML TTS tutorial
    static final String PROMPT2a = "<pron sym=\"H EH 1 L OW & W ER 1 L D\"> hello world </pron>";
    // Without the "&", the pron tag is just ignored (no speech output)
    static final String PROMPT2b = "<pron sym=\"H EH 1 L OW \"> cunt </pron>";

    // Loquendo tags work neither, and "kunt" is pronounced wrong
    static final String PROMPT3 = "You're a \\SAMPA=(k A n t), you know that.";
    static final String PROMPT3a = "\\SAMPA=(k A n t)";
    static final String PROMPT3b = "You're a kunt, you know that.";
    static final String PROMPT3c = "You're a cunt, you know that.";

    // TODO phonemes ignored in 4a - loquendo ignored, MS error
    static final String PROMPT4a = "<speak version=“1.0” xml:lang=“en”><phoneme ph=“t&#x259;mei&#x325;&#x27E;ou&#x325;”>cunt</phoneme></speak>";
    // - 4b works with MS voice but phonemes are ignored, COM-error with LQ voices, after revert of C++ lib something is
    // pronounced but I don't know what it s
    static final String PROMPT4b = "<speak version=\"1.0\" xml:lang=\"en\"><phoneme ph=\"&#x2A7;&#xe6;&#x254;&#x2C8;&#x2D0;\">cunt</phoneme>cunt</speak>";
    // Tomato example from https://www.w3.org/TR/speech-synthesis11/#S3.1.10 -> speaks tomato for MS voices -> Right!
    // Phoneme ignored by Loquendo, must activate something, look into programmer guide
    static final String PROMPT4c = "<speak version=\"1.0\" xml:lang=\"en\"><phoneme alphabet=\"ipa\" ph=\"t&#x259;mei&#x325;&#x27E;ou&#x325;\"> cunt </phoneme> cunt </speak>";

    String prompt = PROMPT4c;
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
