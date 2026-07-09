package teaselib.core.ai.deepspeech;

import java.util.Locale;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import teaselib.core.ai.TeaseLibAI;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;

@TestInstance(Lifecycle.PER_CLASS)
abstract class DeepSpeechRecognizerAbstractTest {

    private TeaseLibAI teaseLibAI;
    SpeechRecognitionEvents events;
    protected DeepSpeechRecognizer deepSpeechRecognizer;

    @BeforeAll
    public void init() {
        teaseLibAI = new TeaseLibAI();
        deepSpeechRecognizer = new DeepSpeechRecognizer(Locale.ENGLISH);
        events = new SpeechRecognitionEvents();
        deepSpeechRecognizer.startEventLoop(events);

    }

    @AfterAll
    public void cleanup() {
        deepSpeechRecognizer.close();
        teaseLibAI.close();
    }

}
