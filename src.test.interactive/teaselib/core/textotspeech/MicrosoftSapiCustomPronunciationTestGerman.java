package teaselib.core.textotspeech;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.texttospeech.TextToSpeech;
import teaselib.core.texttospeech.TextToSpeechTest;
import teaselib.core.texttospeech.Voice;
import teaselib.core.util.Environment;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MicrosoftSapiCustomPronunciationTestGerman {
    private static final Logger logger = LoggerFactory.getLogger(TextToSpeechTest.class);

    static final String VOICE = "MSTTS_V110_deDE_Katja";

    static TextToSpeech textToSpeech;
    static Voice voice;

    @BeforeClass
    public static void initSpeech() {
        Assume.assumeTrue(Environment.SYSTEM == Environment.Windows);
        textToSpeech = new TextToSpeech();
        assertTrue(textToSpeech.isReady());

        Map<String, Voice> voices = textToSpeech.getVoices();
        assertTrue(voices.size() > 0);
        logger.info(voices.keySet().toString());

        voice = voices.get(VOICE);
        textToSpeech.setVoice(voice);
    }

    @Test
    public void testPronunciationSSMLPhonemeTagIPAOtherLanguage() throws InterruptedException {
        // https://www.w3.org/TR/speech-synthesis/#S3.1.9
        // https://easypronunciation.com/de/french-phonetic-transcription-converter#result
        textToSpeech.speak("<speak version=\"1.0\" xml:lang=\"de\">Jawohl, Madame.</speak>");
        textToSpeech.speak(
                "<speak version=\"1.0\" xml:lang=\"de\">Jawohl, <phoneme alphabet=\"ipa\" ph=\"madam\"> Madame. </phoneme> </speak>");
    }

}
