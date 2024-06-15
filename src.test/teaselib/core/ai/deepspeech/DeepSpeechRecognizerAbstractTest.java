package teaselib.core.ai.deepspeech;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import teaselib.core.ai.TeaseLibAI;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;

import java.util.Locale;

@TestInstance(Lifecycle.PER_CLASS)
abstract class DeepSpeechRecognizerAbstractTest {

    private TeaseLibAI teaseLibAI;
    SpeechRecognitionEvents events;
    protected DeepSpeechRecognizer deepSpeechRecognizer;

    @BeforeAll
    public void init() {
        teaseLibAI = new TeaseLibAI();
        deepSpeechRecognizer = new DeepSpeechRecognizer(Locale.ENGLISH);
        deepSpeechRecognizer.setMaxAlternates(SpeechRecognitionImplementation.MAX_ALTERNATES_DEFAULT);
        events = new SpeechRecognitionEvents();
        deepSpeechRecognizer.startEventLoop(events);

    }

    @AfterAll
    public void cleanup() {
        deepSpeechRecognizer.close();
        teaseLibAI.close();
    }

}
