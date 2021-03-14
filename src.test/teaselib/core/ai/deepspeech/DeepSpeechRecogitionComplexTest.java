package teaselib.core.ai.deepspeech;

import static org.junit.jupiter.api.Assertions.*;
import static teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils.*;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import teaselib.core.speechrecognition.srgs.PhraseString;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Intention;
import teaselib.core.ui.Prompt;

class DeepSpeechRecogitionComplexTest extends DeepSpeechRecognitionAbstractTest {

    @Test
    void testOptionalStart() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Of course"), new Choice("Yes Of course"));

        assertRecognized(inputMethod, choices, "Of course", new Prompt.Result(0));
        assertRecognized(inputMethod, choices, "Yes of course", new Prompt.Result(1));

        // TODO no reduced confidence for hypothesis
        assertRejected(inputMethod, choices, "Of");
        assertRejected(inputMethod, choices, "Not");
        assertRejected(inputMethod, choices, "Foo bar");
    }

    @Test
    void testOptionalStartOptionalRule() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Yes Of course", "Yes Of course", "Of course", "Yes Of course"));

        assertRecognized(inputMethod, choices, "Of course", new Prompt.Result(0));
        assertRecognized(inputMethod, choices, "Yes Of course", new Prompt.Result(0));

        // TODO no reduced confidence for hypothesis
        assertRejected(inputMethod, choices, "Of");
        assertRejected(inputMethod, choices, "Yes");

        assertRejected(inputMethod, choices, "Foo bar");
    }

    @Test
    void testOptionalEnd() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Of course"), new Choice("Of course not"));

        assertRecognized(inputMethod, choices, "Of course", new Prompt.Result(0));
        assertRecognized(inputMethod, choices, "Of course not", new Prompt.Result(1));

        assertRejected(inputMethod, choices, "Of");
        assertRejected(inputMethod, choices, "Not");
    }

    @Test
    void testOptionalMiddle() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Of course miss"), new Choice("Of course not miss"));

        assertRecognized(inputMethod, choices, "Of course Miss", new Prompt.Result(0));
        assertRecognized(inputMethod, choices, "Of course not Miss", new Prompt.Result(1));

        // TODO no reduced confidence for hypothesis
        assertRejected(inputMethod, choices, "Of");
        assertRejected(inputMethod, choices, "Not");
    }

    @Test
    void testWordSplit() {
        String[] abc = PhraseString.words("a_b-c");
        assertEquals(3, abc.length);

        String[] abc_ = PhraseString.words("a:b-c_");
        assertEquals(3, abc_.length);

        String[] _abc = PhraseString.words("_a:b-c");
        assertEquals(3, _abc.length);

        String[] _abc_ = PhraseString.words("_a:b-c_");
        assertEquals(3, _abc_.length);
    }

    @Test
    void testPunctationMarks() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Oh Really?"), new Choice("?THese;_:are+a/lOT@(of)puncTAtion-maRkS!"),
                new Choice("No I haven't"));
        assertRecognized(inputMethod, choices, "Oh really", new Prompt.Result(0));
        assertRecognized(inputMethod, choices, "theSE aRe A Lot Of PUnctaTion mArks", new Prompt.Result(1));
        assertRecognized(inputMethod, choices, "No I haven't", new Prompt.Result(2));
    }

    @Test
    void testRejected() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Foo"));
        assertRejected(inputMethod, choices, "Bar");
    }

    @Test
    void testIDontHaveIt() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("I have it"), new Choice("I don't have it"));

        assertRecognized(inputMethod, choices, "I have it", new Prompt.Result(0));
        assertRecognized(inputMethod, choices, "I don't have it", new Prompt.Result(1));
        assertRejected(inputMethod, choices, "have it");
    }

    @Test
    void testOptionalEnd2() throws InterruptedException {
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
    void testSingleChoiceMultiplePhrasesAreDistinct() throws InterruptedException {
        Choices choices = singleChoiceMultiplePhrasesAreDistinct();
        assertRecognized(inputMethod, choices, "Yes Miss of course", new Prompt.Result(0));
        assertRecognized(inputMethod, choices, "Of course Miss", new Prompt.Result(0));

        // TODO Accepted by SAPI SRGS recognizer in Relaxed mode
        assertRejected(inputMethod, choices, "Of course");

        // Accepted as Intention.Chat because DeepSpeech emulation provides correct confidence
        Choices chat = as(choices, Intention.Chat);
        assertRecognizedAsHypothesis(inputMethod, chat, "Yes Miss", new Prompt.Result(0));

        // TODO Rejected by SAPI SRGS recognizer in Relaxed mode
        assertRecognizedAsHypothesis(inputMethod, chat, "Miss", new Prompt.Result(0));

        // accepted as completed, whereas TeaseLibSRGS rejects the incomplete phrase with timeout
        // - accepted as hypothesis in recognitionRejected
        // TODO reject if not matching phrase completely, then use hypothesis
        Choices confirm = as(choices, Intention.Confirm);
        assertRecognizedAsHypothesis(inputMethod, confirm, "Yes Miss", new Prompt.Result(0));

        // TODO SRGS Relaxed accepts this with higher probability as Intention.Decide
        assertRecognizedAsHypothesis(inputMethod, confirm, "Of course", new Prompt.Result(0));
    }

}
