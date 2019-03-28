package teaselib.core.textotspeech;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import teaselib.core.configuration.Configuration;
import teaselib.core.events.Event;
import teaselib.core.speechrecognition.Confidence;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognitionControl;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.speechrecognition.srgs.Phrases;
import teaselib.core.texttospeech.PronunciationDictionary;
import teaselib.core.texttospeech.TextToSpeech;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SpeechRecognitionPronunciationDictionaryTest {

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

    @Test
    public void testSpeechRecognitionPronunciation() throws InterruptedException, IOException {
        PronunciationDictionary pronunciationDictionary = new PronunciationDictionary(
                new File(getClass().getResource("pronunciation").getPath()));
        TextToSpeech textToSpeech = TextToSpeech.allSystemVoices();
        textToSpeech.initPhoneticDictionary(pronunciationDictionary);

        try (SpeechRecognizer speechRecognizer = new SpeechRecognizer(new Configuration());) {
            SpeechRecognition speechRecognition = speechRecognizer.get(Locale.US);
            CountDownLatch completed = new CountDownLatch(1);
            List<String> choices = Arrays.asList("Bereit", "Madame");

            Event<SpeechRecognitionControl, SpeechRecognizedEventArgs> speechRecognized = (sender,
                    eventArgs) -> completed.countDown();

            speechRecognition.events.recognitionCompleted.add(speechRecognized);
            try {
                speechRecognition.startRecognition(Phrases.of(choices), Confidence.Normal);
                speechRecognition.emulateRecogntion("Hello");
                completed.await();
            } finally {
                speechRecognition.events.recognitionCompleted.remove(speechRecognized);
                SpeechRecognition.completeSpeechRecognitionInProgress();
            }
        }
    }
}
