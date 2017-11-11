package teaselib.core.textotspeech;

import static org.junit.Assert.*;

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

import teaselib.Actor;
import teaselib.Mood;
import teaselib.core.Configuration;
import teaselib.core.ResourceLoader;
import teaselib.core.events.Event;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.texttospeech.PronunciationDictionary;
import teaselib.core.texttospeech.TextToSpeech;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.core.texttospeech.Voice;
import teaselib.core.util.Environment;
import teaselib.test.DebugSetup;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MicrosoftSapiPronunciationDictionaryTest {
    private static final Logger logger = LoggerFactory.getLogger(MicrosoftSapiPronunciationDictionaryTest.class);

    // MS SAPI voices issues:
    // - Loquendo voices ignore any hints about pronunciation (SSML, Loquendo tags etc.)
    // - MS mobile voices (added via extra category) ignore user dictionary
    // - only official SAPI voices consult the user dictionary
    // - creating pronunciation with UPS is tedious
    // - Speech Server Zira Pro pronounces "cum" wrong (because it's older?) but sounds better.

    // MS Speech recognition issues:
    // - Cannot use UPS phonemes in AddWordTransition - interpreted as lexical
    // - Cannot compile SRGS xml at runtime - only static
    // - Speech recognition seems to ignore the user dictionary
    // (cannot say "Hello" in the recognition test)
    // So:
    // - must implement Loquendo TTS provider to correct Loquendo wrong pronunciations
    // - must implement .NET Microsoft.Speech TTS provider dll to correct pronunciation
    // - At leasat .NET allows to use SSML

    // SpLexicon User lexicon entries are applied to Microsoft non-mobile voices only,
    // but the user dictionary entries are persisted system wide per user
    // - > However, the file can be deleted to reset the changes,
    // just look into C:\Users\xxx\AppData\Roaming\Microsoft\Speech\Files\UserLexicons\

    // Microsoft UPS phonemes can't be inlined into addWordTransition either,
    // so there's no point in using SpLexicon for speech recognition
    // -> no phoneme lexicon for Microsoft SAPI
    // So far the only way seems to implement .NET Microsoft.Speech and use SRGS xml.

    static final String VOICE_GERMAN = "TTS_MS_DE-DE_HEDDA_11.0";

    // Windows 10
    static final String VOICE_ENGLISH_WINDOWS_DESKTOP_CORRECT = "TTS_MS_EN-US_ZIRA_11.0";
    static final String VOICE_ENGLISH_WINDOWS_MOBILE_CORRECT = "MSTTS_V110_enUS_ZiraM";

    // Available via Speech Server 11 (older)
    static final String VOICE_ENGLISH_SPPECH_SERVER_WRONG_BUT_BETTER_QUALITY = "TTS_MS_en-US_ZiraPro_11.0";

    // Loquendo
    static final String VOICE_LOQUENDO_WRONG_BUT_BUT_WRONG = "LTTS7Kate";

    static TextToSpeech textToSpeech;

    @BeforeClass
    public static void initSpeech() {
        Assume.assumeTrue(Environment.SYSTEM == Environment.Windows);

        textToSpeech = new TextToSpeech();

        Map<String, Voice> voices = textToSpeech.getVoices();
        assertTrue(voices.size() > 1);
        logger.info(voices.keySet().toString());
    }

    // Cum with "u" -> wrong but can be replaced with "Come".
    @Test
    public void testPronunciationOfCum() throws InterruptedException {
        textToSpeech.speak(textToSpeech.getVoices().get(VOICE_ENGLISH_WINDOWS_DESKTOP_CORRECT), "Cum.");

        textToSpeech.speak(textToSpeech.getVoices().get(VOICE_ENGLISH_WINDOWS_MOBILE_CORRECT), "Cum.");

        textToSpeech.speak(textToSpeech.getVoices().get(VOICE_ENGLISH_SPPECH_SERVER_WRONG_BUT_BETTER_QUALITY), "Cum.");

        textToSpeech.speak(textToSpeech.getVoices().get(VOICE_LOQUENDO_WRONG_BUT_BUT_WRONG), "Cum.");
    }

    @Test
    public void testPronunciationCorrection() throws InterruptedException {
        Configuration config = DebugSetup.getConfiguration();
        config.set(TextToSpeechPlayer.Settings.Pronunciation, getClass().getResource("pronunciation").getPath());

        TextToSpeechPlayer tts = new TextToSpeechPlayer(config);
        Actor actor = new Actor("Mrs.Foo", Voice.Female, Locale.forLanguageTag("en-uk"));

        tts.acquireVoice(actor, new ResourceLoader(getClass()));
        tts.speak(actor, "Cum.", Mood.Neutral);
    }

    @Test
    public void testPronunciationDefaultsForZiraPro() throws InterruptedException {
        Configuration config = DebugSetup.getConfiguration();
        config.set(TextToSpeechPlayer.Settings.Pronunciation, new File("defaults/pronunciation").getAbsolutePath());

        TextToSpeechPlayer tts = new TextToSpeechPlayer(config);
        Actor actor = new Actor("Mrs.Foo", Voice.Female, Locale.forLanguageTag("en-uk"));

        tts.acquireVoice(actor, new ResourceLoader(getClass()));
        tts.speak(actor, "Cum.", Mood.Neutral);
    }

    @Test
    public void testSAPIPronuncuation() throws InterruptedException, IOException {
        PronunciationDictionary pronunciationDictionary = new PronunciationDictionary(
                new File(getClass().getResource("pronunciation").getPath()));
        textToSpeech.initPhoneticDictionary(pronunciationDictionary);
        // Speaks "Madame" as "Hello" as defined in the dictionary
        textToSpeech.speak(textToSpeech.getVoices().get(VOICE_GERMAN), "Jawohl, Madame.");
    }

    @Test
    public void testSpeechRecognitionPronuncuation() throws InterruptedException, IOException {
        PronunciationDictionary pronunciationDictionary = new PronunciationDictionary(
                new File(getClass().getResource("pronunciation").getPath()));
        textToSpeech.initPhoneticDictionary(pronunciationDictionary);

        SpeechRecognition speechRecognition = SpeechRecognizer.instance.get(Locale.US);
        CountDownLatch completed = new CountDownLatch(1);
        List<String> choices = Arrays.asList("Bereit", "Madame");

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
