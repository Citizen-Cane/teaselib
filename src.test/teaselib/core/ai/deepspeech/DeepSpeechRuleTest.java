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
