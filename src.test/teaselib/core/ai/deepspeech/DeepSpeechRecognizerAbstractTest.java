package teaselib.core.ai.deepspeech;

import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import teaselib.core.ai.TeaseLibAI;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;

@TestInstance(Lifecycle.PER_CLASS)
abstract class DeepSpeechRecognizerAbstractTest {

    private TeaseLibAI teaseLibAI;
    SpeechRecognitionEvents events;
    protected DeepSpeechRecognizer deepSpeechRecognizer;

    @BeforeAll
    @Before
    public void init() {
        teaseLibAI = new TeaseLibAI();
        deepSpeechRecognizer = new DeepSpeechRecognizer(Locale.ENGLISH);
        deepSpeechRecognizer.setMaxAlternates(SpeechRecognitionImplementation.MAX_ALTERNATES_DEFAULT);
        events = new SpeechRecognitionEvents();
        deepSpeechRecognizer.startEventLoop(events);

    }

    @AfterAll
    @After
    public void cleanup() {
        deepSpeechRecognizer.close();
        teaseLibAI.close();
    }

}
