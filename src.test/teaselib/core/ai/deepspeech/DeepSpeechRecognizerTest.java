package teaselib.core.ai.deepspeech;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import teaselib.core.ai.TeaseLibAI;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Intention;

class DeepSpeechRecognizerTest {

    @Test
    void testLanguageFallbackWorks() throws InterruptedException {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            SpeechRecognitionEvents events = new SpeechRecognitionEvents();

            Locale australianEnglish = new Locale("en", "au");
            try (DeepSpeechRecognizer deepSpeechRecognizer = new DeepSpeechRecognizer(australianEnglish)) {
                deepSpeechRecognizer.startEventLoop(events);
                Choices choices = new Choices(australianEnglish, Intention.Confirm, new Choice("foobar"));
                deepSpeechRecognizer.prepare(choices).accept(deepSpeechRecognizer);
                deepSpeechRecognizer.emulateRecognition("foobar");
                SpeechRecognitionTestUtils.await(0, deepSpeechRecognizer, events);
                assertTrue(deepSpeechRecognizer.getException().isEmpty());
            }
        }
    }

}
