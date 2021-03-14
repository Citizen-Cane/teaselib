package teaselib.core.ai.deepspeech;

import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import teaselib.core.ai.TeaseLibAI;
import teaselib.core.speechrecognition.SpeechRecognitionInputMethod;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils;

@TestInstance(Lifecycle.PER_CLASS)
abstract class DeepSpeechRecognitionAbstractTest {

    private TeaseLibAI teaseLibAI;
    private SpeechRecognizer recognizer;
    protected SpeechRecognitionInputMethod inputMethod;

    @BeforeAll
    @Before
    public void init() {
        teaseLibAI = new TeaseLibAI();
        recognizer = SpeechRecognitionTestUtils.getRecognizer(DeepSpeechRecognizer.class);
        inputMethod = new SpeechRecognitionInputMethod(recognizer);
    }

    @AfterAll
    @After
    public void cleanup() {
        recognizer.close();
        teaseLibAI.close();
    }

}