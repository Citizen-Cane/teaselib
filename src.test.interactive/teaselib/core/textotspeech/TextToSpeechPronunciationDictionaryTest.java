package teaselib.core.textotspeech;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import teaselib.Actor;
import teaselib.Mood;
import teaselib.core.ResourceLoader;
import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.texttospeech.PronunciationDictionary;
import teaselib.core.texttospeech.TextToSpeech;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.core.texttospeech.Voice;
import teaselib.core.util.Environment;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TextToSpeechPronunciationDictionaryTest {
    private static final Actor MS_ZIRA_PRO = new Actor("MS Zira Pro", Voice.Female, Locale.forLanguageTag("en-us"));
    private static final Actor LOQUENDO_KATE = new Actor("Loquendo Kate", Voice.Female, Locale.forLanguageTag("en-uk"));
    private static final Actor LOQUENDO_ALLISON = new Actor("Loquendo Allison", Voice.Female,
            Locale.forLanguageTag("en-uk"));

    // MS SAPI voices issues:
    // - Loquendo voices ignore any hints about pronunciation (SSML, Loquendo tags etc.)
    // - MS mobile voices (added via extra category) ignore user dictionary
    // - only official SAPI voices consult the user dictionary
    // - creating pronunciation with UPS is tedious
    // - Speech Server Zira Pro pronounces "cum" wrong (because it's older?) but overall sounds better.

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
        TextToSpeech textToSpeech = TextToSpeech.allSystemVoices();
        textToSpeech.speak(textToSpeech.getVoices().get(VOICE_ENGLISH_WINDOWS_DESKTOP_CORRECT), prompt);
        textToSpeech.speak(textToSpeech.getVoices().get(VOICE_ENGLISH_WINDOWS_MOBILE_CORRECT), prompt);
        textToSpeech.speak(textToSpeech.getVoices().get(VOICE_ENGLISH_SPPECH_SERVER_ZIRA_PRO_WRONG_BUT_BETTER_QUALITY),
                prompt);
        textToSpeech.speak(textToSpeech.getVoices().get(VOICE_LOQUENDO_UK), prompt);
        textToSpeech.speak(textToSpeech.getVoices().get(VOICE_LOQUENDO_CORRECT_US), prompt);
    }

    @Test
    public void testPronunciationCorrectionWithTestDictionary() throws InterruptedException {
        String prompt = "For pity's sake.";
        TextToSpeechPlayer tts = getTTSPlayer(getClass().getResource("pronunciation").getPath());

        speak(MS_ZIRA_PRO, prompt, tts);
        speak(LOQUENDO_KATE, prompt, tts);
        speak(LOQUENDO_ALLISON, prompt, tts);
    }

    @Test
    public void testPronunciationDefaultsWithProductionDictionary() throws InterruptedException {
        String prompt = "For pity's sake.";
        TextToSpeechPlayer tts = getTTSPlayer(new File("defaults/pronunciation").getAbsolutePath());

        speak(MS_ZIRA_PRO, prompt, tts);
        speak(LOQUENDO_KATE, prompt, tts);
        speak(LOQUENDO_ALLISON, prompt, tts);
    }

    private static TextToSpeechPlayer getTTSPlayer(String path) {
        Configuration config = DebugSetup.getConfiguration();
        config.set(TextToSpeechPlayer.Settings.Pronunciation, path);
        return new TextToSpeechPlayer(config);
    }

    private void speak(Actor actor, String prompt, TextToSpeechPlayer tts) throws InterruptedException {
        tts.acquireVoice(actor, new ResourceLoader(getClass()));
        tts.speak(actor, prompt, Mood.Neutral);
    }

    @Test
    public void testSAPIPronuncuationWithPhoneticDictionary() throws InterruptedException, IOException {
        PronunciationDictionary pronunciationDictionary = new PronunciationDictionary(
                new File(getClass().getResource("pronunciation").getPath()));
        TextToSpeech textToSpeech = TextToSpeech.allSystemVoices();
        textToSpeech.initPhoneticDictionary(pronunciationDictionary);
        // Speaks "Madame" as "Hello" as defined in the dictionary
        textToSpeech.speak(textToSpeech.getVoices().get(VOICE_GERMAN), "Jawohl, Madame.");
    }
}
