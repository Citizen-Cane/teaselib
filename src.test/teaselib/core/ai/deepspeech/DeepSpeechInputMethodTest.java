package teaselib.core.ai.deepspeech;

import static teaselib.core.ai.deepspeech.DeepSpeechTestData.*;

import java.nio.file.Path;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import teaselib.core.ai.TeaseLibAI;
import teaselib.core.speechrecognition.SpeechRecognitionInputMethod;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Intention;
import teaselib.core.ui.Prompt;

public class DeepSpeechInputMethodTest {
    private TeaseLibAI teaseLibAI;
    private SpeechRecognizer recognizer;
    private SpeechRecognitionInputMethod inputMethod;

    @Before
    public void init() {
        teaseLibAI = new TeaseLibAI();
        recognizer = SpeechRecognitionTestUtils.getRecognizer(DeepSpeechRecognizer.class);
        inputMethod = new SpeechRecognitionInputMethod(recognizer);
        ;
    }

    @After
    public void cleanup() {
        recognizer.close();
        teaseLibAI.close();
    }

    @Test
    public void testExperienceProovesThis() throws InterruptedException {
        testExpected(AUDIO_2830_3980_0043_RAW);
    }

    @Test
    public void testWhyShouldOneHaltOnTheWay() throws InterruptedException {
        testExpected(AUDIO_4507_16021_0012_RAW);
    }

    @Test
    public void testYourPowerIsSufficientIsaid() throws InterruptedException {
        testExpected(AUDIO_8455_210777_0068_RAW);
    }

    @Test
    public void testExperienceProovesThisGroundTruth() throws InterruptedException {
        testGroundTruth(AUDIO_2830_3980_0043_RAW);
    }

    @Test
    public void testWhyShouldOneHaltOnTheWayGroundTruth() throws InterruptedException {
        testGroundTruth(AUDIO_4507_16021_0012_RAW);
    }

    @Test
    public void testYourPowerIsSufficientIsaidGroundTruth() throws InterruptedException {
        testGroundTruth(AUDIO_8455_210777_0068_RAW);
    }

    public void testExpected(DeepSpeechTestData testData) throws InterruptedException {
        test(testData.audio, testData.expected, testData.actual);
    }

    public void testGroundTruth(DeepSpeechTestData testData) throws InterruptedException {
        test(testData.audio, testData.expected, testData.expected);
    }

    public void test(Path audio, String expected, String actual) throws InterruptedException {
        SpeechRecognitionTestUtils.assertRecognized( //
                inputMethod, //
                new Choices(Locale.ENGLISH, Intention.Confirm, new Choice(expected, actual)), //
                audio.toString(), //
                new Prompt.Result(0));
    }

}
