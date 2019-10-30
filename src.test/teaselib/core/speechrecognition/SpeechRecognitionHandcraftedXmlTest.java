package teaselib.core.speechrecognition;

import static org.junit.Assert.assertEquals;
import static teaselib.core.speechrecognition.SpeechRecognitionTestUtils.awaitResult;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;

import teaselib.core.ResourceLoader;
import teaselib.core.speechrecognition.implementation.TeaseLibSRGS;
import teaselib.core.speechrecognition.srgs.StringSequence;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.SpeechRecognitionInputMethod;
import teaselib.core.util.Stream;

public class SpeechRecognitionHandcraftedXmlTest {
    private static final Choices Foobar = new Choices(
            Arrays.asList(new Choice("My name is Foo"), new Choice("My name is Bar"), new Choice("My name is Foobar")));
    private static final Confidence confidence = Confidence.High;

    private static void assertRecognized(String resource, String emulatedRecognitionResult, Prompt.Result expected)
            throws IOException, InterruptedException {
        emulateSpeechRecognition(resource, emulatedRecognitionResult, expected);
    }

    private static void assertRecognized(String resource, String emulatedRecognitionResult, Prompt.Result expected,
            Prompt.Result.Accept mode) throws IOException, InterruptedException {
        emulateSpeechRecognition(resource, emulatedRecognitionResult, expected, mode);
    }

    private static void assertRejected(String resource, String emulatedRecognitionResult)
            throws IOException, InterruptedException {
        emulateSpeechRecognition(resource, emulatedRecognitionResult, null, Prompt.Result.Accept.Multiple);
    }

    private static void assertRejected(String resource, String emulatedRecognitionResult, Prompt.Result.Accept mode)
            throws IOException, InterruptedException {
        emulateSpeechRecognition(resource, emulatedRecognitionResult, null, mode);
    }

    private static void emulateSpeechRecognition(String resource, String emulatedRecognitionResult,
            Prompt.Result expected) throws IOException, InterruptedException {
        emulateSpeechRecognition(resource, emulatedRecognitionResult, expected, Prompt.Result.Accept.Multiple);
    }

    private static void emulateSpeechRecognition(String resource, String emulatedRecognitionResult,
            Prompt.Result expected, Prompt.Result.Accept mode) throws IOException, InterruptedException {
        assertEquals("Emulated speech may not contain punctation: '" + emulatedRecognitionResult + "'",
                StringSequence.splitWords(emulatedRecognitionResult).stream().collect(Collectors.joining(" ")),
                emulatedRecognitionResult);
        ResourceLoader resources = new ResourceLoader(SpeechRecognitionHandcraftedXmlTest.class);
        byte[] xml = Stream.toByteArray(resources.get(resource));
        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSRGS.class) {
            @Override
            byte[] srgs(Choices choices) {
                mapper = index -> index;
                return xml;
            }
        };
        SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence, Optional.empty());
        Prompt prompt = new Prompt(Foobar, Arrays.asList(inputMethod), mode);

        prompt.lock.lockInterruptibly();
        try {
            inputMethod.show(prompt);
            awaitResult(sr, prompt, emulatedRecognitionResult, expected);
        } finally {
            prompt.lock.unlock();
            sr.close();
        }
    }

    @Test
    public void testMicrosoftSRGSExampleCities() throws InterruptedException, IOException {
        String resource = "srgs/experimental/cities_srg.xml";
        // Not recognized since the example doesn't contain rules
        // that match the TeaseLib srgs speech recognition naming scheme
        assertRejected(resource, "I would like to fly from Miami to Los Angeles");
        assertRejected(resource, "I would like to fly from Paris to Moscow");
    }

    @Test
    public void testHandcraftedChildren() throws InterruptedException, IOException {
        String resource = "srgs/experimental/handcrafted_children_srg.xml";
        assertRecognized(resource, "Please Miss one less May I", new Prompt.Result(1, 1));
        assertRecognized(resource, "Please Miss two more May I", new Prompt.Result(2, 1));
        assertRecognized(resource, "Please Miss two more okay", new Prompt.Result(2, 0));
        assertRejected(resource, "Please Miss three more May I");
        assertRejected(resource, "Please Miss one more");
    }

    @Test
    public void testHandcraftedSiblings() throws InterruptedException, IOException {
        String resource = "srgs/experimental/handcrafted_siblings_srg.xml";
        assertRecognized(resource, "Please Miss one less May I", new Prompt.Result(1, 1));
        assertRecognized(resource, "Please Miss two more May I", new Prompt.Result(2, 1));
        assertRecognized(resource, "Please Miss two more okay", new Prompt.Result(2, 0));
        assertRejected(resource, "Please Miss three more May I");
        assertRejected(resource, "Please Miss one more");
    }

    @Test
    public void testHandcraftedMultipleChoices() throws InterruptedException, IOException {
        String resource = "srgs/handcrafted_multiple_choices_srg.xml";
        assertRecognized(resource, "Please Miss two more strokes May I", new Prompt.Result(2, 1));
        assertRecognized(resource, "Please Miss one less strokes May I", new Prompt.Result(1, 1));
        assertRecognized(resource, "Please Miss two more strokes May I", new Prompt.Result(2, 1));
        assertRecognized(resource, "Please Miss two more strokes okay", new Prompt.Result(2, 0));
        assertRejected(resource, "Please Miss three more strokes May I");
        assertRejected(resource, "Please Miss one more");

    }

    @Test
    public void testHandcraftedCommonMiddle() throws InterruptedException, IOException {
        String resource = "srgs/handcrafted_common_middle_srg.xml";
        assertRecognized(resource, "Yes Miss I've spurted off", new Prompt.Result(0, 0));
        assertRecognized(resource, "No Miss I didn't spurt off", new Prompt.Result(1, 1));
        assertRejected(resource, "No Miss I've spurted off");
        assertRejected(resource, "Yes Miss I didn't spurt off");
    }

    @Test
    public void testHandcraftedSameChoiceCommonStart() throws InterruptedException, IOException {
        String resource = "srgs/handcrafted_common_start.xml";
        assertRecognized(resource, "Dear Mistress I've spurted my load", new Prompt.Result(0));
        assertRecognized(resource, "Dear Mistress I didn't spurt off", new Prompt.Result(1));
        assertRecognized(resource, "Miss I've spurted my load", new Prompt.Result(0));
        assertRecognized(resource, "Miss I didn't spurt off", new Prompt.Result(1));
        assertRejected(resource, "Okay Miss I've spurted my load");
        assertRejected(resource, "Dear Mistress I've spurted off");
        assertRejected(resource, "Dear Miss I've spurted my load");
    }

    @Test
    public void testHandcraftedSameChoiceCommonEnd() throws InterruptedException, IOException {
        String resource = "srgs/handcrafted_common_end.xml";
        assertRecognized(resource, "I've spurted my load Dear Mistress", new Prompt.Result(0));
        assertRecognized(resource, "I didn't spurt off Dear Mistress", new Prompt.Result(1));
        assertRejected(resource, "I've spurted my load Miss");
        assertRejected(resource, "I didn't spurt my load Dear Mistress");
    }

    @Test
    public void testHandcraftedSameChoiceCommonStartEnd() throws InterruptedException, IOException {
        String resource = "srgs/handcrafted_common_start_end.xml";
        assertRecognized(resource, "Dear Mistress I've spurted my load Miss", new Prompt.Result(0));
        assertRecognized(resource, "Dear Mistress I didn't spurt off Miss", new Prompt.Result(1));
        assertRejected(resource, "Miss I've spurted my load Miss");
        assertRejected(resource, "Dear Mistress I didn't spurt off Misstress");
    }

    /**
     * Demonstrate optional item elements in srgs xml
     */
    @Test
    public void testHandcraftedOptionalCommonRule() throws InterruptedException, IOException {
        String resource = "srgs/handcrafted_optional_common_rule.xml";

        assertRecognized(resource, "Yes Miss of course", new Prompt.Result(0));
        assertRecognized(resource, "No of course", new Prompt.Result(1));
        assertRecognized(resource, "No Miss of course", new Prompt.Result(1));

        assertRecognized(resource, "Yes Miss of course miss", new Prompt.Result(0, 0));
        assertRecognized(resource, "Yes of course Miss", new Prompt.Result(0, 0));

        assertRecognized(resource, "No of course not Miss", new Prompt.Result(1, 1));
        assertRecognized(resource, "No Miss of course not miss", new Prompt.Result(1, 1));
        assertRecognized(resource, "No of course not Miss", new Prompt.Result(1, 1));
    }

    /**
     * Demonstrate use of special rule to start phrase building after first slice:
     * <p>
     * not recognized because there are multiple results.
     */
    @Test
    public void testHandcraftedDelayedPhraseStartWithGarbage() throws InterruptedException, IOException {
        String srgs = "srgs/handcrafted_delayed_phrase_start_with_garbage.xml";
        assertRejected(srgs, "Yes of course", Prompt.Result.Accept.AllSame);
        assertRejected(srgs, "Of course Miss", Prompt.Result.Accept.AllSame);
        assertRejected(srgs, "Yes of course Miss", Prompt.Result.Accept.AllSame);
        assertRejected(srgs, "of course", Prompt.Result.Accept.AllSame);
        assertRejected(srgs, "any", Prompt.Result.Accept.AllSame);
    }

    /**
     * Demonstrate use of special rule to start phrase building after first slice:
     * <p>
     * not recognized since using special=GARBAGE in a choice ruleRef results in multiple recognitions, as "Yes" is
     * recognized as "Yes" and also as Garbage.
     */
    @Test
    public void testHandcraftedDelayedPhraseStartWithGarbageRuleRef() throws InterruptedException, IOException {
        String srgs = "srgs/handcrafted_delayed_phrase_start_with_garbage_RuleRef.xml";
        assertRejected(srgs, "Yes of course", Prompt.Result.Accept.AllSame);
        assertRejected(srgs, "Of course Miss", Prompt.Result.Accept.AllSame);
        assertRejected(srgs, "Yes of course Miss", Prompt.Result.Accept.AllSame);
        assertRejected(srgs, "of course", Prompt.Result.Accept.AllSame);
        assertRejected(srgs, "any", Prompt.Result.Accept.AllSame);
    }

    /**
     * Demonstrate use of special rule to start phrase building after first slice:
     * <p>
     * Results in a single recognition with two children (on for each phrase chunk).
     */
    @Test
    public void testHandcraftedDelayedPhraseStartWithNull() throws InterruptedException, IOException {
        String srgs = "srgs/handcrafted_delayed_phrase_start_with_null.xml";
        assertRecognized(srgs, "Yes of course", new Prompt.Result(0), Prompt.Result.Accept.AllSame);
        assertRecognized(srgs, "Of course Miss", new Prompt.Result(1), Prompt.Result.Accept.AllSame);
        assertRejected(srgs, "Yes of course Miss", Prompt.Result.Accept.AllSame);
        assertRejected(srgs, "of course", Prompt.Result.Accept.AllSame);
        assertRejected(srgs, "any", Prompt.Result.Accept.AllSame);
    }

    /**
     * Demonstrate use of special rule to start phrase building after first slice:
     * <p>
     * Results in a single recognition with three children (on for each phrase chunk), the third originates from the
     * special=NULL rule (Yes or Miss not spoken).
     * <p>
     * Perfect recognition of all child rules because the rules not spoken result in a NULL rule with the intended
     * choice index.
     */
    @Test
    public void testHandcraftedDelayedPhraseStartWithNullRuleRef() throws InterruptedException, IOException {
        String srgs = "srgs/handcrafted_delayed_phrase_start_with_null_RuleRef.xml";
        assertRecognized(srgs, "Yes of course", new Prompt.Result(0), Prompt.Result.Accept.AllSame);
        assertRecognized(srgs, "Of course Miss", new Prompt.Result(1), Prompt.Result.Accept.AllSame);
        assertRejected(srgs, "Yes of course Miss", Prompt.Result.Accept.AllSame);
        assertRejected(srgs, "of course", Prompt.Result.Accept.AllSame);
        assertRejected(srgs, "any", Prompt.Result.Accept.AllSame);
    }

    /**
     * Demonstrate mixed common and choice item elements in srgs xml
     */
    @Test
    public void testHandcraftedMixedCommonRule() throws InterruptedException, IOException {
        String resource = "srgs/handcrafted_mixed_commom_rule.xml";

        assertRejected(resource, "Yes Miss of course");
        assertRejected(resource, "No Miss of course");

        assertRecognized(resource, "Yes of course Miss", new Prompt.Result(0, 0));
        assertRecognized(resource, "No of course not Miss", new Prompt.Result(1, 1));

    }

}
