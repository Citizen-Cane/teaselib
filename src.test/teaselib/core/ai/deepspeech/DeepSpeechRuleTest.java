package teaselib.core.ai.deepspeech;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import teaselib.core.ai.TeaseLibAI;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Intention;

/**
 * @author someone
 *
 */
public class DeepSpeechRuleTest {

    @Test
    public void testLanguageFallbackWorks() throws InterruptedException {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            SpeechRecognitionEvents events = new SpeechRecognitionEvents();

            AtomicReference<SpeechRecognizedEventArgs> speechRecognized = new AtomicReference<>();
            CountDownLatch signal = new CountDownLatch(1);
            events.recognitionCompleted.add(e -> {
                speechRecognized.set(e);
                signal.countDown();
            });
            Locale australianEnglish = new Locale("en", "au");
            try (DeepSpeechRecognizer deepSpeechRecognizer = new DeepSpeechRecognizer(australianEnglish)) {
                deepSpeechRecognizer.startEventLoop(events);
                Choices choices = new Choices(australianEnglish, Intention.Confirm, new Choice("foobar"));
                deepSpeechRecognizer.prepare(choices).accept(deepSpeechRecognizer);
                deepSpeechRecognizer.emulateRecognition("foobar");
                SpeechRecognitionTestUtils.await(0, deepSpeechRecognizer, speechRecognized, signal);
                assertTrue(deepSpeechRecognizer.getException().isEmpty());
            }
        }
    }

    @Test
    public void testYesMissWithRealworldResults() throws InterruptedException {
        testEmulateText("Yes Miss", """
                yes\n
                yes\n
                yes i\n
                yes miss\n
                yes is\n
                yes it\n
                yes m\n
                yes a\n
                yes as\n
                yes s
                """);

        testEmulateText("Yes Miss", """
                        yes
                        i guess
                        as
                        yet
                        guess
                        is
                        get
                        his
                        at
                        eyes
                """);
    }

    @Test
    public void testMissingWordAtEnd() throws InterruptedException {
        Rule rule = emulateText("Yes Miss I have it", "yes miss i have");
        assertEquals(5, rule.children.size());
    }

    @Test
    public void testMissingWordsAtEnd() throws InterruptedException {
        Rule rule = emulateText("Yes Miss I have it", "yes miss i");
        assertEquals(4, rule.children.size());
    }

    @Test
    public void testMissingWordInTheMiddle() throws InterruptedException {
        Rule rule = emulateText("Yes dear Misstress I have it here", "yes i have");
        assertEquals(5, rule.children.size());
    }

    // Testdata taken from failing test when audio stream buffers weren't cleared after recognition
    // "paris" decomposes to "par is" - contains the missing word, better than nothing
    static final String detectedSpeech = """
            your paris efficient i said
            your paris sufficient i said
            you paris efficient i said
            your parents efficient i said
            your paris efficient y said
            """;

    // When all tests are run without resetting the audio stream, "your paris efficient i said" is recognized
    // [[your, paris, efficient, i, said] confidence=1.0]
    // [[your, paris, sufficient, i, said] confidence=0.9432924]
    // [[you, paris, efficient, i, said] confidence=0.84364516]
    // [[your, parents, efficient, i, said] confidence=0.8344438]
    // [[your, paris, efficient, y, said] confidence=0.82584846]

    // TODO In this example is matched as "i", resulting in sufficient" to be matched in "said" -> low confidence
    // TODO emulation code does not reset audio stream

    // // When one test are run, "your PART is sufficient i said" is recognized - expected
    // [[your, part, is, sufficient, i, said] confidence=1.0]
    // [[your, paris, sufficient, i, said] confidence=0.82849437]
    // [[your, part, is, sufficient, i, said] confidence=0.76291585]
    // [[your, part, as, sufficient, i, said] confidence=0.7604105]
    // [[your, part, is, sufficient, said] confidence=0.75178033]

    // test catches samples from the previous recognition -> clear buffers, when to call clear() - impl coreect?

    @Test
    public void testMissingWordAndWrongRecognitionGroundTruth() throws InterruptedException {
        Rule rule = emulateText(DeepSpeechTestData.AUDIO_8455_210777_0068_RAW.groundTruth, detectedSpeech);
        assertEquals(5, rule.children.size());
        assertNull(rule.children.get(3).text, "\"is\" should be recognized as null rule");
        assertNull(rule.children.get(4).text, "\"sufficient\" should be recognized as child 4");
        assertNull(rule.children.get(5).text, "\"I\" should be recognized as child 5");
        SpeechRecognitionTestUtils.assertConfidence(rule, Intention.Decide);
    }

    @Test
    public void testMissingWordAndWrongRecognitionActual() throws InterruptedException {
        Rule rule = emulateText(DeepSpeechTestData.AUDIO_8455_210777_0068_RAW.actual, detectedSpeech);
        assertEquals(5, rule.children.size());
        assertNull(rule.children.get(3).text, "\"is\" should be recognized as null rule");
        assertNull(rule.children.get(4).text, "\"sufficient\" should be recognized as child 4");
        assertNull(rule.children.get(5).text, "\"I\" should be recognized as child 5");
        SpeechRecognitionTestUtils.assertConfidence(rule, Intention.Decide);
    }

    private static Rule emulateText(String expected, String actual) throws InterruptedException, UnsatisfiedLinkError {
        return testEmulateText(expected, actual);
    }

    @Test
    public void testExperienceProovesThisAsText() throws InterruptedException {
        testEmulateText("experience prooves this",
                "experience prooves this\nexperience prooves that\nthe experience proofs it");
    }

    private static Rule testEmulateText(String choice, String speech)
            throws InterruptedException, UnsatisfiedLinkError {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            SpeechRecognitionEvents events = new SpeechRecognitionEvents();

            AtomicReference<SpeechRecognizedEventArgs> speechRecognized = new AtomicReference<>();
            CountDownLatch signal = new CountDownLatch(1);
            events.recognitionCompleted.add(e -> {
                speechRecognized.set(e);
                signal.countDown();
            });

            Rule best;
            try (DeepSpeechRecognizer deepSpeechRecognizer = new DeepSpeechRecognizer(Locale.ENGLISH)) {
                deepSpeechRecognizer.startEventLoop(events);
                Choices choices = new Choices(Locale.ENGLISH, Intention.Confirm, new Choice(choice));
                deepSpeechRecognizer.prepare(choices).accept(deepSpeechRecognizer);
                deepSpeechRecognizer.emulateRecognition(speech);
                best = SpeechRecognitionTestUtils.await(0, deepSpeechRecognizer, speechRecognized, signal);
            }

            assertNotNull(speechRecognized.get());
            return best;
        }
    }

}
