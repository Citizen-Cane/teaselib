package teaselib.core.textotspeech;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.texttospeech.TextToSpeech;
import teaselib.core.texttospeech.Voice;
import teaselib.core.util.Environment;

public class LoquendoSapiCustomPronunciation {

    private static final Logger logger = LoggerFactory.getLogger(LoquendoSapiCustomPronunciation.class);

    // Cum with "u" -> wrong
    // Cunt right
    static final String MS_VOICE = "TTS_MS_en-US_ZiraPro_11.0";

    // Cum = come -> right
    // Cunt with "u" -> wrong
    static final String LQ_VOICE = "LTTS7Allison";

    static final String PROMPT0a = "You're a stupid cunt!";
    static final String PROMPT0b = "<emph>cunt</emph>!";

    // IPA pronunciation ignored
    // Unlike 4e, "cunt" is pronounced correctly inside the tag
    static final String PROMPT1a = "<P DISP=\"cunt\" PRON=\"H EH 1 L OW & W ER 1 L D\"> cunt </P> ";
    static final String PROMPT1b = "<P DISP=\"cunt\" PRON=\"h eh 1 l ow\"> cunt</P> ";
    // not recognized as pronunciation, maybe only for recognition?
    static final String PROMPT1c = " <P>/cunt/cunt/H EH 1 L OW;</P> ";

    // COM-Error because of the & - &amp doesn't work
    static final String PROMPT2a = "<pron sym=\"H EH 1 L OW & W ER 1 L D\"> cunt </pron> ";
    // Works with MS voices, ignored by Loquendo
    static final String PROMPT2b = "<pron sym=\"H EH 1 L OW W ER 1 L D\"> cunt </pron> ";

    // Loquendo tags work neither, and "kunt" is pronounced wrong
    static final String PROMPT3a = "You're a \\SAMPA=(k A n t), you know that.";
    static final String PROMPT3b = " \\SAMPA=(k A n t) ";
    static final String PROMPT3c = " \\sampa=(k A n t) ";
    static final String PROMPT3d = "You're a kunt, you know that.";
    static final String PROMPT3e = "You're a cunt, you know that.";

    static final String PROMPT3f = "\\sampa=x-sampa;(\"p_hleIs) ";
    static final String PROMPT3g = "\\ipa(le g&#640;&#712;&#593;&#771;&#658;) ";

    // TODO phonemes ignored in 4a for both
    static final String PROMPT4a = "<speak version=â€œ1.0â€� xml:lang=â€œenâ€�><phoneme ph=â€œt&#x259;mei&#x325;&#x27E;ou&#x325;â€�>cunt</phoneme> </speak>";
    // - 4b works with MS, COM-error with LQ voices, maybe because the & is not preceeded by a letter
    static final String PROMPT4b = "<speak version=\"1.0\" xml:lang=\"en\"><phoneme  ph=\"&#x2A7;&#xe6;&#x254;&#x2C8;&#x2D0;\">cunt</phoneme> </speak>";
    // Tomato example from https://www.w3.org/TR/speech-synthesis11/#S3.1.10 -> speaks tomato for MS voices -> Right!
    // Phoneme ignored by Loquendo, must activate something, look into programmer guide
    // In Prompt 4c-4e, "cunt" inside the phoneme tag is pronounced wrong by Loquendo,"cunt" outside the phoneme tag is
    // pronounced better, but not yet correct, MS pronounces both occurrences correctly
    static final String PROMPT4c = "<speak version=\"1.0\" xml:lang=\"en\"><phoneme alphabet=\"ipa\" ph=\"t&#x259;mei&#x325;&#x27E;ou&#x325;\"> cunt </phoneme> cunt </speak>";
    static final String PROMPT4d = "<speak version=\"1.1\" xml:lang=\"en\"><phoneme alphabet=\"ipa\" ph=\"t&#x259;mei&#x325;&#x27E;ou&#x325;\"> cunt </phoneme> cunt </speak>";
    static final String PROMPT4e = "<speak version=\"1.1\" xml:lang=\"en\"><phoneme alphabet=\"ipa\" ph=\"tÉ™meiÌ¥É¾ouÌ¥\"> cunt </phoneme> cunt </speak>";
    static final String PROMPT4f = "<speak version=\"1.1\" xml:lang=\"en\">You're a stupid <phoneme alphabet=\"ipa\" ph=\"kant\"> cunt</phoneme>, you know that?</speak>";
    static final String PROMPT4f1 = "<speak version=\"1.1\" xml:lang=\"en\">You're a stupid cunt, you know that?</speak>";
    static final String PROMPT4f2 = "You're a stupid cunt, you know that?";

    static final String PROMPT4g = "<speak version=\"1.0\" xml:lang=\"en\"> You're a stupid \\SAMPA=(k A n t) cunt </speak>";
    static final String PROMPT4h = "<speak version=\"1.1\" xml:lang=\"en\"> You're a stupid \\SAMPA=(k A n t) cunt </speak>";

    static final String PROMPT5a = "\\item=Aahh. This is \\speed=70\\pitch=30 a marked up text for \\spell=yes LTTS.";

    // Needs space after \item and \n before \SAMPA, but still doesn't work in sapi
    static final String PROMPT6a = " \\item=Humph . You're a stupid \n \\SAMPA=(k A n t).\\item=Aahh .";
    static final String PROMPT6b = "<speak version=\"1.0\" xml:lang=\"en\"> \\item=Humph . You're a stupid \n \\SAMPA=(k A n t).\\item=Aahh . </speak>";

    // After all, using \SAMPA (on the next line) results in bad word stressing - original sounds better.

    // No difference between the two, at least the <emph> tag can be put in the middle of the sentence
    static final String PROMPT6c = "<speak version=\"1.0\" xml:lang=\"en\"> You're a stupid cunt! </speak>";
    static final String PROMPT6d = "<speak version=\"1.0\" xml:lang=\"en\"> You're a stupid <emph>cunt</emph>! </speak>";

    // So much better with LQQ7Allison
    static final String PROMPT7a = "You aren't allowed to cum without permission!";

    // Loquendo still has the best voices (at least of the ones I have).
    // - Controls and effects of Loquendo Director are nice
    // - \ipa(...) words without a newline (unlike >sampa(...) which seems to have a parsing bug)
    // -> replacing words with ipa pronunciation breaks sentences nevertheless because the TTS engine doesn't know
    // which exact word is pronounced - that information is missing, so in fact \ipa(ËˆstjuË�pÉªd kÊŒnt) improves
    // pronunciation but makes the sentence sound less good.

    String prompt = PROMPT4f2;
    String voiceGuid = LQ_VOICE;

    @Test
    public void testCustomPronunciation() throws InterruptedException {
        Assume.assumeTrue(Environment.SYSTEM == Environment.Windows);
        TextToSpeech textToSpeech = new TextToSpeech();

        Map<String, Voice> voices = textToSpeech.getVoices();
        assertTrue(voices.size() > 1);
        logger.info(voices.keySet().toString());

        Voice voice = voices.get(voiceGuid);
        logger.info(voice + " -> " + prompt);

        textToSpeech.speak(voice, prompt);
    }

}
