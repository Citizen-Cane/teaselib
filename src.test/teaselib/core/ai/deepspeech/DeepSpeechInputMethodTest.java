package teaselib.core.ai.deepspeech;

import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Intention;
import teaselib.core.ui.Prompt;

class DeepSpeechInputMethodTest extends DeepSpeechInputMethodAbstractTest {

    static Choices choices = new Choices(Locale.ENGLISH, Intention.Confirm) {
        private static final long serialVersionUID = 1L;

        {
            tests().forEach(test -> add(new Choice(test.groundTruth, test.groundTruth)));
        }
    };

    static Stream<DeepSpeechTestData> tests() {
        return DeepSpeechTestData.tests.stream();
    }

    @ParameterizedTest
    @MethodSource("tests")
    void testExpectedAudio(DeepSpeechTestData testData) throws InterruptedException {
        test(testData.audio.toString(), DeepSpeechTestData.tests.indexOf(testData));
    }

    @ParameterizedTest
    @MethodSource("tests")
    void testExpectedText(DeepSpeechTestData testData) throws InterruptedException {
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
