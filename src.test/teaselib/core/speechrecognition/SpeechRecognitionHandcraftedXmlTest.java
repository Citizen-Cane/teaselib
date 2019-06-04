package teaselib.core.speechrecognition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.Test;

import teaselib.core.ResourceLoader;
import teaselib.core.speechrecognition.implementation.TeaseLibSRGS;
import teaselib.core.speechrecognition.srgs.Phrases;
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

    private static void assertRejected(String resource, String emulatedRecognitionResult)
            throws IOException, InterruptedException {
        emulateSpeechRecognition(resource, emulatedRecognitionResult, null);
    }

    private static void emulateSpeechRecognition(String resource, String emulatedRecognitionResult,
            Prompt.Result expected) throws IOException, InterruptedException {
        assertEquals("Emulated speech may not contain punctation: '" + emulatedRecognitionResult + "'",
                StringSequence.splitWords(emulatedRecognitionResult).stream().collect(Collectors.joining(" ")),
                emulatedRecognitionResult);
        ResourceLoader resources = new ResourceLoader(SpeechRecognitionHandcraftedXmlTest.class);
        String xml = Stream.toString(resources.get(resource));
        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSRGS.class) {
            @Override
            String srgs(Phrases phrases) {
                return xml;
            }
        };

        SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence, Optional.empty());
        Prompt prompt = new Prompt(Foobar, Arrays.asList(inputMethod));

        prompt.lock.lockInterruptibly();
        try {
            inputMethod.show(prompt);
            awaitResult(sr, prompt, emulatedRecognitionResult, expected);
        } finally {
            prompt.lock.unlock();
            sr.close();
        }
    }

    private static void awaitResult(SpeechRecognition sr, Prompt prompt, String emulatedText, Prompt.Result expected)
            throws InterruptedException {
        sr.emulateRecogntion(emulatedText);
        boolean dismissed = prompt.click.await(3, TimeUnit.SECONDS);
        if (!dismissed) {
            prompt.dismiss();
        }
        if (expected != null) {
            assertTrue("Expected recognition:: \"" + emulatedText + "\"", dismissed);
            assertEquals(expected, prompt.result());
        } else {
            assertFalse("Expected rejected: \"" + emulatedText + "\"", dismissed);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMicrosoftSRGSExampleCities() throws InterruptedException, IOException {
        String resource = "srgs/experimental/cities_srg.xml";
        assertRecognized(resource, "I would like to fly from Miami to Los Angeles", new Prompt.Result(3));
        // fails since the example doesn't contain rules that match the teaselib srgs speech recognition naming scheme
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
        String resource = "srgs/handcrafted_optional_commom_rule.xml";

        assertRecognized(resource, "Yes Miss of course", new Prompt.Result(0));
        assertRecognized(resource, "No of course", new Prompt.Result(1));

        assertRecognized(resource, "Yes Miss of course miss", new Prompt.Result(0, 0));
        assertRecognized(resource, "Yes of course Miss", new Prompt.Result(0, 0));

        assertRecognized(resource, "No of course not Miss", new Prompt.Result(1, 1));
        assertRecognized(resource, "No Miss of course not miss", new Prompt.Result(1, 1));
        assertRecognized(resource, "No of course not Miss", new Prompt.Result(1, 1));

        // assertRejected(resource, "No Miss of course");
        // assertRejected(resource, "No of course not Miss");
    }

}
