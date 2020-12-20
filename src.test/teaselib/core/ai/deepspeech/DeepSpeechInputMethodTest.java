package teaselib.core.ai.deepspeech;

import java.util.Locale;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import teaselib.core.ai.TeaseLibAI;
import teaselib.core.speechrecognition.SpeechRecognitionInputMethod;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Intention;
import teaselib.core.ui.Prompt;

@TestInstance(Lifecycle.PER_CLASS)
public class DeepSpeechInputMethodTest {

    static Choices choices = new Choices(Locale.ENGLISH, Intention.Confirm) {
        private static final long serialVersionUID = 1L;

        {
            tests().forEach(test -> add(new Choice(test.groundTruth, test.groundTruth)));
        }
    };

    private TeaseLibAI teaseLibAI;
    private SpeechRecognizer recognizer;
    private SpeechRecognitionInputMethod inputMethod;

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

    static Stream<DeepSpeechTestData> tests() {
        return DeepSpeechTestData.tests.stream();
    }

    @ParameterizedTest
    @MethodSource("tests")
    public void testExpectedAudio(DeepSpeechTestData testData) throws InterruptedException {
        test(testData.audio.toString(), DeepSpeechTestData.tests.indexOf(testData));
    }

    @ParameterizedTest
    @MethodSource("tests")
    public void testExpectedText(DeepSpeechTestData testData) throws InterruptedException {
        test(testData.groundTruth, DeepSpeechTestData.tests.indexOf(testData));
    }

    private void test(String speech, int choice) throws InterruptedException {
        SpeechRecognitionTestUtils.assertRecognized( //
                inputMethod, //
                choices, //
                speech, //
                new Prompt.Result(choice));
    }

}
