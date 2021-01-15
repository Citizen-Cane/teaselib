package teaselib.core.ai.deepspeech;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static teaselib.core.speechrecognition.SpeechRecognitionInputMethod.confidence;
import static teaselib.core.util.ExceptionUtil.asRuntimeException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import teaselib.core.ai.TeaseLibAI;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Intention;

public class DeepSpeechRecognizerTest {

    static Stream<DeepSpeechTestData> tests() {
        return DeepSpeechTestData.tests.stream();
    }

    @ParameterizedTest
    @MethodSource("tests")
    public void testExpectedAudio(DeepSpeechTestData testData) throws InterruptedException {
        testAudio(testData);
    }

    public static void testAudio(DeepSpeechTestData testData) throws InterruptedException {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            SpeechRecognitionEvents events = new SpeechRecognitionEvents();

            AtomicReference<SpeechRecognizedEventArgs> speechRecognized = new AtomicReference<>();
            CountDownLatch signal = new CountDownLatch(1);
            events.recognitionCompleted.add(e -> {
                speechRecognized.set(e);
                signal.countDown();
            });
            try (DeepSpeechRecognizer deepSpeechRecognizer = new DeepSpeechRecognizer(Locale.ENGLISH)) {
                deepSpeechRecognizer.startEventLoop(events);
                Choices choices = new Choices(Locale.ENGLISH, Intention.Confirm, new Choice(testData.groundTruth));
                deepSpeechRecognizer.prepare(choices).accept(deepSpeechRecognizer);
                deepSpeechRecognizer.emulateRecognition(testData.audio.toString());
                await(choices, 0, deepSpeechRecognizer, speechRecognized, signal, testData.groundTruth);
            }
        }
    }

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
                await(choices, 0, deepSpeechRecognizer, speechRecognized, signal, choices.get(0).display);
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
    public void testExperiencePrroovesThisAsText() throws InterruptedException {
        testEmulateText("experience prooves this",
                "experience prooves this\nexperience prooves that\nthe experience proofs it");
    }

    private void testEmulateText(String choice, String speech) throws InterruptedException, UnsatisfiedLinkError {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            SpeechRecognitionEvents events = new SpeechRecognitionEvents();

            AtomicReference<SpeechRecognizedEventArgs> speechRecognized = new AtomicReference<>();
            CountDownLatch signal = new CountDownLatch(1);
            events.recognitionCompleted.add(e -> {
                speechRecognized.set(e);
                signal.countDown();
            });
            try (DeepSpeechRecognizer deepSpeechRecognizer = new DeepSpeechRecognizer(Locale.ENGLISH)) {
                deepSpeechRecognizer.startEventLoop(events);
                Choices choices = new Choices(Locale.ENGLISH, Intention.Confirm, new Choice(choice));
                deepSpeechRecognizer.prepare(choices).accept(deepSpeechRecognizer);
                deepSpeechRecognizer.emulateRecognition(speech);
                await(choices, 0, deepSpeechRecognizer, speechRecognized, signal, choices.get(0).display);
                assertNotNull(speechRecognized.get());
            }
        }
    }

    private static void await(Choices choices, int choice, DeepSpeechRecognizer deepSpeechRecognizer,
            AtomicReference<SpeechRecognizedEventArgs> event, CountDownLatch signal, String speech)
            throws InterruptedException {
        if (signal.await(10, TimeUnit.MINUTES)) {
            List<Rule> rules = event.get().result;
            assertFalse("Audio file result expected - path correct?", rules.isEmpty());
            Rule best = rules.get(0);
            assertEquals(1, best.indices.size());
            assertEquals(choice, best.indices.iterator().next().intValue());
            assertTrue(best.probability > confidence(choices.intention).probability);
        } else {
            Optional<Throwable> failure = deepSpeechRecognizer.getException();
            if (failure.isPresent()) {
                throw asRuntimeException(failure.get());
            } else {
                fail("Speech detection timed out");
            }
        }
    }

}
