package teaselib.core.ai.deepspeech;

import static teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils.as;
import static teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils.assertRecognized;
import static teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils.assertRecognizedAsHypothesis;
import static teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils.assertRejected;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Intention;
import teaselib.core.ui.Prompt;

class DeepSpeechRecogitionComplexTest extends DeepSpeechRecognitionAbstractTest {

    @Test
    public void testOptionalStart() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Of course"), new Choice("Yes Of course"));

        assertRecognized(inputMethod, choices, "Of course", new Prompt.Result(0));
        assertRecognized(inputMethod, choices, "Yes of course", new Prompt.Result(1));

        // TODO throws IndexOutOfBounds exception in processing thread -> all tests fail
        assertRejected(inputMethod, choices, "Of");
        assertRejected(inputMethod, choices, "Not");
        assertRejected(inputMethod, choices, "Foo bar");
    }

    @Test
    public void testOptionalEnd() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Of course"), new Choice("Of course not"));

        assertRecognized(inputMethod, choices, "Of course", new Prompt.Result(0));
        assertRecognized(inputMethod, choices, "Of course not", new Prompt.Result(1));

        assertRejected(inputMethod, choices, "Of");
        assertRejected(inputMethod, choices, "Not");
    }

    @Test
    public void testOptionalMiddle() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Of course miss"), new Choice("Of course not miss"));

        assertRecognized(inputMethod, choices, "Of course Miss", new Prompt.Result(0));
        assertRecognized(inputMethod, choices, "Of course not Miss", new Prompt.Result(1));

        assertRejected(inputMethod, choices, "Of");
        assertRejected(inputMethod, choices, "Not");
    }

    @Test
    public void testRejected() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Foo"));
        assertRejected(inputMethod, choices, "Bar");
    }

    @Test
    public void testOptionalEnd2() throws InterruptedException {
        String[] yes = { "Of course", "of course Miss" };
        String[] no = { "Of course not", "of course not Miss" };
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("of course", "of course", yes), new Choice("of course not", "of course not", no));

        assertRecognized(inputMethod, choices, "Of course", new Prompt.Result(0));
        assertRecognized(inputMethod, choices, "of course Miss", new Prompt.Result(0));
        assertRecognized(inputMethod, choices, "Of course not Miss", new Prompt.Result(1));

        assertRecognizedAsHypothesis(inputMethod, choices, "of course", new Prompt.Result(0));
        assertRecognizedAsHypothesis(inputMethod, choices, "Of course not", new Prompt.Result(1));

        assertRejected(inputMethod, choices, "Miss");
    }

    private static Choices singleChoiceMultiplePhrasesAreDistinct() {
        String[] yes = { "Yes Miss, of course", "Of course, Miss" };
        return new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Yes #title, of course", "Yes Miss, of course", yes));
    }

    @Test
    public void testSingleChoiceMultiplePhrasesAreDistinct() throws InterruptedException {
        Choices choices = singleChoiceMultiplePhrasesAreDistinct();
        assertRecognized(inputMethod, choices, "Yes Miss of course", new Prompt.Result(0));
        assertRecognized(inputMethod, choices, "Of course Miss", new Prompt.Result(0));

        // Accepted as Intention.Chat because DeepSpeech emulation provides correct confidence
        Choices hypothized = as(choices, Intention.Chat);
        assertRecognizedAsHypothesis(inputMethod, hypothized, "Yes Miss", new Prompt.Result(0));
        // accepted as completed, whereas TeaseLibSRGS rejects the incomplete phrase with timeout
        // - accepted as hypothesis in recognitionRejected
        // TODO reject if not matching phrase completely, then use hypothesis

        assertRejected(inputMethod, choices, "Of course");
        assertRejected(inputMethod, choices, "Miss");
    }

}
