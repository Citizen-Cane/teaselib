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

public class LoquendoSapiCustomPronunciation {

    private static final Logger logger = LoggerFactory.getLogger(TextToSpeechTest.class);

    // Cum with "u" -> wrong
    // Cunt right
    static final String MS_VOICE = "TTS_MS_en-US_ZiraPro_11.0";

    // Cum = come -> right
    // Cunt with "u" -> wrong
    static final String LQ_VOICE = "LTTS7Allison";

    static final String PROMPT0a = "You're a stupid cunt!";
    static final String PROMPT0b = "<emph>cunt</emph>!";

    // IPA pronunciation ignored
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
    static final String PROMPT4a = "<speak version=“1.0” xml:lang=“en”><phoneme ph=“t&#x259;mei&#x325;&#x27E;ou&#x325;”>cunt</phoneme> </speak>";
    // - 4b works with MS, COM-error with LQ voices, maybe because the & is not preceeded by a letter
    static final String PROMPT4b = "<speak version=\"1.0\" xml:lang=\"en\"><phoneme  ph=\"&#x2A7;&#xe6;&#x254;&#x2C8;&#x2D0;\">cunt</phoneme> </speak>";
    // Tomato example from https://www.w3.org/TR/speech-synthesis11/#S3.1.10 -> speaks tomato for MS voices -> Right!
    // Phoneme ignored by Loquendo, must activate something, look into programmer guide
    static final String PROMPT4c = "<speak version=\"1.0\" xml:lang=\"en\"><phoneme alphabet=\"ipa\" ph=\"t&#x259;mei&#x325;&#x27E;ou&#x325;\"> cunt </phoneme> cunt </speak>";
    static final String PROMPT4d = "<speak version=\"1.1\" xml:lang=\"en\"><phoneme alphabet=\"ipa\" ph=\"t&#x259;mei&#x325;&#x27E;ou&#x325;\"> cunt </phoneme> cunt </speak>";
    static final String PROMPT4e = "<speak version=\"1.1\" xml:lang=\"en\"><phoneme alphabet=\"ipa\" ph=\"təmei̥ɾou̥\"> cunt </phoneme> cunt </speak>";

    static final String PROMPT4f = "<speak version=\"1.0\" xml:lang=\"en\"> \\SAMPA=(k A n t) hello </speak>";
    static final String PROMPT4g = "<speak version=\"1.1\" xml:lang=\"en\"> \\SAMPA=(k A n t) hello </speak>";

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
    // - \ipa(...) workds without a newline (unlike >sampa(...) which seems to have a parsing bug)
    // -> replacing words with ipa pronounciation breaks sentences nevertheless because the TTS engine doesn't know
    // which exact word is pronounced - that information is missing, so in fact \ipa(ˈstjuːpɪd kʌnt) improves
    // pronounciation but makes the sentence sound less good.

    String prompt = PROMPT7a;
    String voiceGuid = LQ_VOICE;

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
