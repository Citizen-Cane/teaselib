package teaselib.core.textotspeech;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.events.Event;
import teaselib.core.speechrecognition.Confidence;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.texttospeech.TextToSpeech;
import teaselib.core.texttospeech.Voice;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.util.Environment;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MicrosoftSapiCustomPronunciationTestEnglish {
    private static final Logger logger = LoggerFactory.getLogger(MicrosoftSapiCustomPronunciationTestEnglish.class);

    static final String VOICE_GUID = "TTS_MS_en-US_ZiraPro_11.0";

    static TextToSpeech textToSpeech;

    private static Voice voice;

    @BeforeClass
    public static void initSpeech() {
        Assume.assumeTrue(Environment.SYSTEM == Environment.Windows);
        textToSpeech = TextToSpeech.allSystemVoices();

        Map<String, Voice> voices = textToSpeech.getVoices();
        assertTrue(voices.size() > 1);
        logger.info(voices.keySet().toString());

        voice = voices.get(VOICE_GUID);
    }

    @Test
    public void testPronunciationOfCum() {
        // Cum with "u" -> wrong but can be replaced by "Come".
        textToSpeech.speak(voice, "Cum.");
    }

    @Test
    public void testPronunciationDifferencesOfCumWithSSML() {
        // Cum with "u" -> wrong but can be replaced by "Come".
        // TODO Try whole sentence and phonemes and check whether the melody of the speech is preserved
        textToSpeech.speak(voice, "You aren't allowed to cum without permission.");
        textToSpeech.speak(voice,
                "<speak version=\"1.0\" xml:lang=\"en\">You aren't allowed to <phoneme alphabet=\"ipa\" ph=\"kʌm\"> cum </phoneme> without permission. </speak>");
        textToSpeech.speak(voice, "You aren't allowed to come without permission.");
        // -> corrections are similar
    }

    @Test
    public void testPronunciationOfCunt() {
        textToSpeech.speak(voice, "Stupid cunt!");
    }

    @Test
    public void testPronunciationOfCuntEmphasized() {
        textToSpeech.speak(voice, "Stupid <emph>cunt</emph>!");
    }

    @Test(expected = RuntimeException.class)
    public void revealPronunciationPronAmpersandProblemHelloWorld() {
        // TODO Word boundary operator causes error, &amp doesn't solve the issue
        textToSpeech.speak(voice, "<pron sym=\"H EH 1 L OW & W ER 1 L D\"> replaced </pron> ");
    }

    @Test
    public void testPronunciationSapiPronHelloWorld() {
        // https://msdn.microsoft.com/en-us/library/ms717077(VS.85).aspx#Custom_Pronunciation
        // https://en.wikipedia.org/wiki/Help:Pronunciation_respelling_key
        // https://msdn.microsoft.com/en-us/library/ms717239(v=vs.85).aspx
        // textToSpeech.speak("<pron sym=\"H EH 1 L OW W ER 1 L D\"> replaced </pron> ");
        textToSpeech.speak(voice, "<pron sym=\"h eh 1 l ow   w er 1 l d\"> replaced </pron> ");
    }

    @Test
    public void testPronunciationSAPIPronTagHelloWorld() {
        // https://msdn.microsoft.com/en-us/library/ms717077(VS.85).aspx#Custom_Pronunciation
        // https://en.wikipedia.org/wiki/Help:Pronunciation_respelling_key
        // https://msdn.microsoft.com/en-us/library/ms717239(v=vs.85).aspx
        // textToSpeech.speak("<pron sym=\"H EH 1 L OW W ER 1 L D\"> replaced </pron> ");
        textToSpeech.speak(voice, "<pron sym=\"h eh 1 l ow   w er 1 l d\"> replaced </pron> ");
    }

    @Test
    @Ignore
    public void testPronunciationSAPIDISPTag() {
        // https://msdn.microsoft.com/en-us/library/ms717077(VS.85).aspx#Custom_Pronunciation
        // TODO Not for TTS, maybe just for recognition?
        textToSpeech.speak(voice, "<P DISP=\"replace\" PRON=\"H EH 1 L OW  W ER 1 L D\"> replace </P> ");
        textToSpeech.speak(voice, "<P>/replace/replace/H EH 1 L OW;</P>");
    }

    @Test
    public void testPronunciationSSMLPhonemeTagIPAHelloWorld() {
        // https://www.w3.org/TR/speech-synthesis/#S3.1.9
        // http://lingorado.com/ipa/
        textToSpeech.speak(voice,
                "<speak version=\"1.0\" xml:lang=\"en\"><phoneme alphabet=\"ipa\" ph=\"hɛˈləʊ wɜːld\"> replaced. </phoneme> </speak>");
    }

    @Test
    public void testPronunciationSSMLPhonemeTagIPAOtherLanguageHelloWorld() {
        // https://www.w3.org/TR/speech-synthesis/#S3.1.9
        // http://lingorado.com/ipa/
        textToSpeech.speak(voice,
                "<speak version=\"1.0\" xml:lang=\"en\"><phoneme alphabet=\"ipa\" ph=\"hɛˈləʊ wɜːld\"> replaced. </phoneme> </speak>");
    }

    @Test
    public void testPronunciationSAPIPronTagSpeechRecognitionHelloWorld() throws InterruptedException, IOException {
        try (SpeechRecognizer speechRecognizer = new SpeechRecognizer(
                new Configuration(new DebugSetup().withInput()))) {
            SpeechRecognition speechRecognition = speechRecognizer.get(Locale.US);
            CountDownLatch completed = new CountDownLatch(1);
            // "P DISP=\"replace\" PRON=\"H EH 1 L OW W ER 1 L D\"> replace </P>"
            Choices choices = new Choices(new Choice("<P>/Display/Word/H EH 1 L OW;</P>"));
            Event<SpeechRecognizedEventArgs> event = speechRecognition.events.recognitionCompleted
                    .add(events -> completed.countDown());
            try {
                speechRecognition.startRecognition(choices, Confidence.Normal);
                completed.await();
            } finally {
                speechRecognition.events.recognitionCompleted.remove(event);
            }
        }
    }

}
