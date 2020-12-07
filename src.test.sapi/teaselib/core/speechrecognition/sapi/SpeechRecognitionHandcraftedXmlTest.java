package teaselib.core.speechrecognition.sapi;

import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertEquals;
import static teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils.awaitResult;
import static teaselib.core.util.ExceptionUtil.asRuntimeException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.IntUnaryOperator;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ResourceLoader;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.SpeechRecognitionInputMethod;
import teaselib.core.speechrecognition.SpeechRecognitionNativeImplementation;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.speechrecognition.sapi.TeaseLibSRGS.PreparedChoicesImplementation;
import teaselib.core.speechrecognition.srgs.PhraseString;
import teaselib.core.speechrecognition.srgs.SRGSPhraseBuilder;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.InputMethods;
import teaselib.core.ui.Intention;
import teaselib.core.ui.Prompt;
import teaselib.core.util.Stream;

public class SpeechRecognitionHandcraftedXmlTest {
    static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionHandcraftedXmlTest.class);

    private static final Choices Foobar = new Choices(Locale.ENGLISH, Intention.Decide, //
            new Choice("My name is Foo"), new Choice("My name is Bar"), new Choice("My name is Foobar"));

    private static List<Rule> assertRecognized(String resource, String emulatedRecognitionResult,
            Prompt.Result expected) throws IOException, InterruptedException {
        return emulateSpeechRecognition(resource, emulatedRecognitionResult, expected);
    }

    private static List<Rule> assertRecognized(String resource, String emulatedRecognitionResult,
            Prompt.Result expected, Prompt.Result.Accept mode) throws IOException, InterruptedException {
        return emulateSpeechRecognition(resource, emulatedRecognitionResult, expected, mode);
    }

    private static List<Rule> assertRejected(String resource, String emulatedRecognitionResult)
            throws IOException, InterruptedException {
        return emulateSpeechRecognition(resource, emulatedRecognitionResult, null, Prompt.Result.Accept.Multiple);
    }

    private static List<Rule> assertRejected(String resource, String emulatedRecognitionResult,
            Prompt.Result.Accept mode) throws IOException, InterruptedException {
        return emulateSpeechRecognition(resource, emulatedRecognitionResult, null, mode);
    }

    private static List<Rule> emulateSpeechRecognition(String resource, String emulatedRecognitionResult,
            Prompt.Result expected) throws IOException, InterruptedException {
        return emulateSpeechRecognition(resource, emulatedRecognitionResult, expected, Prompt.Result.Accept.Multiple);
    }

    public static class TestableTeaseLibSRGS extends TeaseLibSRGS {

        public TestableTeaseLibSRGS(Locale locale, SpeechRecognitionEvents events) {
            super(locale, events);
        }

        @Override
        public List<Rule> repair(List<Rule> result) {
            return result;
        }

    }

    private static List<Rule> emulateSpeechRecognition(String resource, String emulatedRecognitionResult,
            Prompt.Result expected, Prompt.Result.Accept mode) throws IOException, InterruptedException {
        assertEquals("Emulated speech may not contain punctation: '" + emulatedRecognitionResult + "'",
                Arrays.stream(PhraseString.words(emulatedRecognitionResult)).collect(joining(" ")),
                emulatedRecognitionResult);

        ResourceLoader resources = new ResourceLoader(SpeechRecognitionHandcraftedXmlTest.class);
        try (InputStream inputStream = resources.get(resource);) {
            byte[] xml = Stream.toByteArray(inputStream);
            try (SpeechRecognizer sR = SpeechRecognitionTestUtils.getRecognizer(TestableTeaseLibSRGS.class);) {
                SpeechRecognition sr = sR.get(Foobar.locale);
                try (SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sR) {
                    @Override
                    public Setup getSetup(Choices choices) {
                        Setup setup = super.getSetup(choices);
                        return () -> {
                            setup.apply();
                            String xmlToString = new String(xml);
                            logger.info("Injecting handcrafted xml\n{}", xmlToString);
                            PreparedChoicesImplementation preparedChoices;
                            try {
                                SRGSPhraseBuilder builder = new SRGSPhraseBuilder(choices,
                                        SpeechRecognitionNativeImplementation.languageCode(Foobar.locale));
                                IntUnaryOperator mapper = builder::map;
                                preparedChoices = ((TestableTeaseLibSRGS) sr.implementation).new PreparedChoicesImplementation(
                                        choices, builder.slices, xml, mapper);
                                sr.apply(preparedChoices);
                            } catch (ParserConfigurationException | TransformerException e) {
                                throw asRuntimeException(e);
                            }

                        };
                    }
                };) {
                    Prompt prompt = new Prompt(Foobar, new InputMethods(inputMethod), mode);
                    return awaitResult(prompt, inputMethod, emulatedRecognitionResult, expected);
                }
            }
        }
    }

    @Test
    public void testMicrosoftSRGSExampleCitiesRejectedSinceItDoesntContainTesaseLibSrRules()
            throws InterruptedException, IOException {
        String resource = "srgs/experimental/cities_srg.xml";
        assertRejected(resource, "I would like to fly from Miami to Los Angeles");
        assertRejected(resource, "I would like to fly from Paris to Moscow");
    }

    // TODO children aren't siblings, so the hierarchy is multiple levels deep
    // -> caused by disabling recursion in RuleIndicesList
    // However in RUle.gather() recursive child -> addAll(child.indices()) breaks a lot of other tests

    @Test
    @Ignore
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
    @Ignore
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
    @Test(expected = AssertionError.class)
    public void testHandcraftedDelayedPhraseStartWithGarbage() throws InterruptedException, IOException {
        String srgs = "srgs/handcrafted_delayed_phrase_start_with_garbage.xml";

        List<Rule> results = new ArrayList<>();
        results.addAll(assertRecognized(srgs, "Yes of course", new Prompt.Result(0), Prompt.Result.Accept.Distinct));
        results.addAll(assertRecognized(srgs, "Of course Miss", new Prompt.Result(1), Prompt.Result.Accept.Distinct));
        assertEquals("sr result contains ambiguous rules since gargabe also matches allowed phrases", 2,
                results.size());

        assertRejected(srgs, "Yes of course Miss", Prompt.Result.Accept.Distinct);
        assertRejected(srgs, "of course", Prompt.Result.Accept.Distinct);
    }

    /**
     * Demonstrate use of special rule to start phrase building after first slice:
     * <p>
     * not recognized since using special=GARBAGE in a choice ruleRef results in multiple recognitions, as "Yes" is
     * recognized as "Yes" and also as Garbage.
     */
    @Test(expected = AssertionError.class)
    public void testHandcraftedDelayedPhraseStartWithGarbageRuleRef() throws InterruptedException, IOException {
        String srgs = "srgs/handcrafted_delayed_phrase_start_with_garbage_RuleRef.xml";

        List<Rule> results = new ArrayList<>();
        results.addAll(assertRecognized(srgs, "Yes of course", new Prompt.Result(0), Prompt.Result.Accept.Distinct));
        results.addAll(assertRecognized(srgs, "Of course Miss", new Prompt.Result(1), Prompt.Result.Accept.Distinct));
        assertEquals("sr result contains ambiguous rules since gargabe also matches allowed phrases", 2,
                results.size());

        assertRejected(srgs, "Yes of course Miss", Prompt.Result.Accept.Distinct);
        assertRejected(srgs, "of course", Prompt.Result.Accept.Distinct);
    }

    /**
     * Demonstrate use of special rule to start phrase building after first slice:
     * <p>
     * Results in a single recognition with two children (on for each phrase chunk).
     */
    @Test
    public void testHandcraftedDelayedPhraseStartWithNull() throws InterruptedException, IOException {
        String srgs = "srgs/handcrafted_delayed_phrase_start_with_null.xml";
        List<Rule> result1 = assertRecognized(srgs, "Yes of course", new Prompt.Result(0),
                Prompt.Result.Accept.Distinct);
        assertEquals(1, result1.size());
        List<Rule> result2 = assertRecognized(srgs, "Of course Miss", new Prompt.Result(1),
                Prompt.Result.Accept.Distinct);
        assertEquals(1, result2.size());

        assertRejected(srgs, "Yes of course Miss", Prompt.Result.Accept.Distinct);
        assertRejected(srgs, "of course", Prompt.Result.Accept.Distinct);
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
        assertRecognized(srgs, "Yes of course", new Prompt.Result(0), Prompt.Result.Accept.Distinct);
        assertRecognized(srgs, "Of course Miss", new Prompt.Result(1), Prompt.Result.Accept.Distinct);
        assertRejected(srgs, "Yes of course Miss", Prompt.Result.Accept.Distinct);
        assertRejected(srgs, "of course", Prompt.Result.Accept.Distinct);
        assertRejected(srgs, "any", Prompt.Result.Accept.Distinct);
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
