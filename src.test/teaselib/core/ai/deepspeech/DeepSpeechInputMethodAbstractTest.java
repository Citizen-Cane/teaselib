package teaselib.core.ai.deepspeech;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import teaselib.core.ai.TeaseLibAI;
import teaselib.core.speechrecognition.SpeechRecognitionInputMethod;
import teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils;

@TestInstance(Lifecycle.PER_CLASS)
abstract class DeepSpeechInputMethodAbstractTest {

    private TeaseLibAI teaseLibAI;
    protected SpeechRecognitionInputMethod inputMethod;

    @BeforeAll
    public void init() {
        teaseLibAI = new TeaseLibAI();
        inputMethod = SpeechRecognitionTestUtils.getInputMethod(DeepSpeechRecognizer.class);
    }

    @AfterAll
    public void cleanup() {
        inputMethod.close();
        teaseLibAI.close();
    }

}
