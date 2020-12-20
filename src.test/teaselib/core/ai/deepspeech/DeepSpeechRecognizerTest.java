package teaselib.core.ai.deepspeech;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static teaselib.core.util.ExceptionUtil.asRuntimeException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
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
                await(deepSpeechRecognizer, speechRecognized, signal, testData.groundTruth);
            }
        }
    }

    @Test
    public void testEmulateText() throws InterruptedException {
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
                Choices choices = new Choices(Locale.ENGLISH, Intention.Confirm, new Choice("experience prooves this"));
                deepSpeechRecognizer.prepare(choices).accept(deepSpeechRecognizer);
                deepSpeechRecognizer.emulateRecognition(
                        "experience prooves this\nexperience prooves that\nthe experience proofs it");
                await(deepSpeechRecognizer, speechRecognized, signal, "experience prooves this");
                assertNotNull(speechRecognized.get());
            }
        }
    }

    private static void await(DeepSpeechRecognizer deepSpeechRecognizer,
            AtomicReference<SpeechRecognizedEventArgs> event, CountDownLatch signal, String speech)
            throws InterruptedException {
        if (signal.await(10, SECONDS)) {
            List<Rule> result = event.get().result;
            assertFalse(result.isEmpty());
            assertEquals(speech, result.get(0).text);
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
