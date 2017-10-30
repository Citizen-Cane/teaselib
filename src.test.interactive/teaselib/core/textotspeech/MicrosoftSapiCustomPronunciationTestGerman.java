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
import teaselib.core.texttospeech.Voice;
import teaselib.core.util.Environment;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MicrosoftSapiCustomPronunciationTestGerman {
    private static final Logger logger = LoggerFactory.getLogger(MicrosoftSapiCustomPronunciationTestGerman.class);

    static final String VOICE_SAPI = "TTS_MS_DE-DE_HEDDA_11.0";
    static final String VOICE_MSSPEECH = "MSTTS_V110_deDE_KatjaM";

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

        voice = voices.get(VOICE_SAPI);
    }

    @Test
    public void testPronunciationSSMLPhonemeTagIPAOtherLanguage() throws InterruptedException {
        // https://www.w3.org/TR/speech-synthesis/#S3.1.9
        // https://easypronunciation.com/de/french-phonetic-transcription-converter#result
        textToSpeech.speak(voice, "<speak version=\"1.0\" xml:lang=\"de\">Jawohl, Madame.</speak>");
        textToSpeech.speak(voice,
                "<speak version=\"1.0\" xml:lang=\"de\">Jawohl, <phoneme alphabet=\"ipa\" ph=\"madam\"> Madame. </phoneme> </speak>");
    }

}
