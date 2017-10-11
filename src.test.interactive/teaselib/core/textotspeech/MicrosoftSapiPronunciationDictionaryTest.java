package teaselib.core.textotspeech;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.events.Event;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.texttospeech.PronunciationDictionary;
import teaselib.core.texttospeech.TextToSpeech;
import teaselib.core.texttospeech.Voice;
import teaselib.core.util.Environment;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MicrosoftSapiPronunciationDictionaryTest {
    private static final Logger logger = LoggerFactory.getLogger(MicrosoftSapiPronunciationDictionaryTest.class);

    // SpLexicon User lexicon entries are applied to Microsoft non-mobile voices,
    // but the user dictionary entries are persisted system wide -> a no-go for us

    // Microsoft UPS phonemes can't be inlined into addWordTransition either,
    // so there's no point in using SpLexicon for that too

    // -> no phoneme lexicon for Microsoft SAPI
    // So far the only way seems to implement .NET Microsoft.Speech and use SRGS xml.

    static final String VOICE = "TTS_MS_DE-DE_HEDDA_11.0";

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
    public void testSAPIPronuncuation() throws InterruptedException, IOException {
        PronunciationDictionary pronunciationDictionary = new PronunciationDictionary(
                new File(getClass().getResource("pronunciation").getPath()));
        textToSpeech.initPhoneticDictionary(pronunciationDictionary);
        textToSpeech.speak("Jawohl, Madame.");
    }

    @Test
    public void testSRPronuncuation() throws InterruptedException, IOException {
        PronunciationDictionary pronunciationDictionary = new PronunciationDictionary(
                new File(getClass().getResource("pronunciation").getPath()));
        textToSpeech.initPhoneticDictionary(pronunciationDictionary);

        SpeechRecognition speechRecognition = SpeechRecognizer.instance.get(Locale.US);
        CountDownLatch completed = new CountDownLatch(1);
        List<String> choices = Arrays.asList("H EH 1 L OW", "W ER 1 L D");

        Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> speechRecognized = new Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs>() {
            @Override
            public void run(SpeechRecognitionImplementation sender, SpeechRecognizedEventArgs eventArgs) {
                completed.countDown();
            }
        };
        speechRecognition.events.recognitionCompleted.add(speechRecognized);
        try {
            speechRecognition.startRecognition(choices, Confidence.Normal);
            speechRecognition.emulateRecogntion("Hello");
            completed.await();
        } finally {
            SpeechRecognition.completeSpeechRecognitionInProgress();
        }
    }
}
