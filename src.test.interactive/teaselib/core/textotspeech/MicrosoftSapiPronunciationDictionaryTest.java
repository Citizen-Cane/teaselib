package teaselib.core.textotspeech;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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

    private static final Actor MS_ZIRA_PRO = new Actor("MS Zira Pro", Voice.Female, Locale.forLanguageTag("en-us"));
    private static final Actor LOQUENDO_KATE = new Actor("Loquendo Kate", Voice.Female, Locale.forLanguageTag("en-uk"));
    private static final Actor LOQUENDO_ALLISON = new Actor("Loquendo Allison", Voice.Female,
            Locale.forLanguageTag("en-uk"));

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
    // - At least .NET allows to use SSML

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
    static final String VOICE_ENGLISH_SPPECH_SERVER_ZIRA_PRO_WRONG_BUT_BETTER_QUALITY = "TTS_MS_en-US_ZiraPro_11.0";

    // Loquendo
    static final String VOICE_LOQUENDO_UK = "LTTS7Kate";
    static final String VOICE_LOQUENDO_CORRECT_US = "LTTS7Allison";

    @BeforeClass
    public static void initSpeech() {
        Assume.assumeTrue(Environment.SYSTEM == Environment.Windows);
    }

    // Cum pronounced with "u" -> not entirely wrong (en-uk?)- but usually pronounced as "Come"
    @Test
    public void testPronunciationOfCum() throws InterruptedException {
        String prompt = "Cum.";
        TextToSpeech textToSpeech = new TextToSpeech();
        textToSpeech.speak(textToSpeech.getVoices().get(VOICE_ENGLISH_WINDOWS_DESKTOP_CORRECT), prompt);
        textToSpeech.speak(textToSpeech.getVoices().get(VOICE_ENGLISH_WINDOWS_MOBILE_CORRECT), prompt);
        textToSpeech.speak(textToSpeech.getVoices().get(VOICE_ENGLISH_SPPECH_SERVER_ZIRA_PRO_WRONG_BUT_BETTER_QUALITY),
                prompt);
        textToSpeech.speak(textToSpeech.getVoices().get(VOICE_LOQUENDO_UK), prompt);
        textToSpeech.speak(textToSpeech.getVoices().get(VOICE_LOQUENDO_CORRECT_US), prompt);
    }

    @Test
    public void testPronunciationCorrectionWithTestDictionary() throws InterruptedException {
        String prompt = "Cum.";
        TextToSpeechPlayer tts = getTTSPlayer(getClass().getResource("pronunciation").getPath());

        speak(MS_ZIRA_PRO, prompt, tts);
        speak(LOQUENDO_KATE, prompt, tts);
        speak(LOQUENDO_ALLISON, prompt, tts);
    }

    @Test
    public void testPronunciationDefaultsWithProductionDictionary() throws InterruptedException {
        String prompt = "Cum.";
        TextToSpeechPlayer tts = getTTSPlayer(new File("defaults/pronunciation").getAbsolutePath());

        speak(MS_ZIRA_PRO, prompt, tts);
        speak(LOQUENDO_KATE, prompt, tts);
        speak(LOQUENDO_ALLISON, prompt, tts);
    }

    private TextToSpeechPlayer getTTSPlayer(String path) {
        Configuration config = DebugSetup.getConfiguration();
        config.set(TextToSpeechPlayer.Settings.Pronunciation, path);
        TextToSpeechPlayer tts = new TextToSpeechPlayer(config);
        tts.load();
        return tts;
    }

    private void speak(Actor actor, String prompt, TextToSpeechPlayer tts) throws InterruptedException {
        tts.acquireVoice(actor, new ResourceLoader(getClass()));
        tts.speak(actor, prompt, Mood.Neutral);
    }

    @Test
    public void testSAPIPronuncuationWithPhoneticDictionary() throws InterruptedException, IOException {
        PronunciationDictionary pronunciationDictionary = new PronunciationDictionary(
                new File(getClass().getResource("pronunciation").getPath()));
        TextToSpeech textToSpeech = new TextToSpeech();
        textToSpeech.initPhoneticDictionary(pronunciationDictionary);
        // Speaks "Madame" as "Hello" as defined in the dictionary
        textToSpeech.speak(textToSpeech.getVoices().get(VOICE_GERMAN), "Jawohl, Madame.");
    }

    @Test
    public void testSpeechRecognitionPronunciation() throws InterruptedException, IOException {
        PronunciationDictionary pronunciationDictionary = new PronunciationDictionary(
                new File(getClass().getResource("pronunciation").getPath()));
        TextToSpeech textToSpeech = new TextToSpeech();
        textToSpeech.initPhoneticDictionary(pronunciationDictionary);

        SpeechRecognition speechRecognition = new SpeechRecognizer(new Configuration()).get(Locale.US);
        CountDownLatch completed = new CountDownLatch(1);
        List<String> choices = Arrays.asList("Bereit", "Madame");

        Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> speechRecognized = (sender,
                eventArgs) -> completed.countDown();

        speechRecognition.events.recognitionCompleted.add(speechRecognized);
        try {
            speechRecognition.startRecognition(choices, Confidence.Normal);
            speechRecognition.emulateRecogntion("Hello");
            completed.await();
        } finally {
            speechRecognition.events.recognitionCompleted.remove(speechRecognized);
            SpeechRecognition.completeSpeechRecognitionInProgress();
        }
    }
}
