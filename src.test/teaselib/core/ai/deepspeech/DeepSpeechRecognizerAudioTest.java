package teaselib.core.ai.deepspeech;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils.assertConfidence;
import static teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils.await;

import java.nio.file.Files;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import teaselib.core.ai.TeaseLibAI;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Intention;

class DeepSpeechRecognizerAudioTest {

    static Stream<DeepSpeechTestData> tests() {
        return DeepSpeechTestData.tests.stream();
    }

    @Test
    void testEmulationStability() throws Throwable {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI();
                DeepSpeechRecognizer deepSpeechRecognizer = new DeepSpeechRecognizer(Locale.ENGLISH)) {
            SpeechRecognitionEvents events = new SpeechRecognitionEvents();
            deepSpeechRecognizer.startEventLoop(events);
            deepSpeechRecognizer.setMaxAlternates(SpeechRecognitionImplementation.MAX_ALTERNATES_DEFAULT);
            Choices choices = new Choices(Locale.ENGLISH, Intention.Confirm, new Choice("Foo bar"));
            deepSpeechRecognizer.prepare(choices).accept(deepSpeechRecognizer);

            for (DeepSpeechTestData testData : DeepSpeechTestData.tests) {
                deepSpeechRecognizer.emulateRecognition(testData.audio.toString());
                Optional<Throwable> exception = deepSpeechRecognizer.getException();
                if (exception.isPresent()) {
                    fail(exception.get());
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("tests")
    void testExpectedAudio(DeepSpeechTestData testData) throws InterruptedException {
        testAudio(testData);
    }

    // 2622 [Speech Recognition event thread] INFO teaselib.core.events.EventSource - recognitionStarted , 0 listeners
    // SpeechRecognitionStartedEventArgs
    // Exception in thread "DeepSpeech Emulation" java.lang.RuntimeException: ring buffer zero elements
    // at teaselib.core.ai.deepspeech.DeepSpeechRecognizer.emulate(Native Method)
    // at teaselib.core.ai.deepspeech.DeepSpeechRecognizer.lambda$3(DeepSpeechRecognizer.java:241)
    // at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1130)
    // at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:630)
    // at java.base/java.lang.Thread.run(Thread.java:832)
    // 3506 [Speech Recognition event thread] INFO teaselib.core.ai.deepspeech.DeepSpeechRecognizer - DeepSpeech results
    // =
    // [[why] confidence=1.0]
    // [[why, s] confidence=0.9463412]
    // [[why, is] confidence=0.8029418]
    // [[why] confidence=0.6653479]
    // [[who, s] confidence=0.65512556]

    static void testAudio(DeepSpeechTestData testData) throws InterruptedException {
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
                deepSpeechRecognizer.setMaxAlternates(SpeechRecognitionImplementation.MAX_ALTERNATES_DEFAULT);
                Choices choices = new Choices(Locale.ENGLISH, Intention.Confirm, new Choice(testData.groundTruth));
                deepSpeechRecognizer.prepare(choices).accept(deepSpeechRecognizer);
                assertTrue(Files.exists(testData.audio), "File not found: " + testData.audio);
                deepSpeechRecognizer.emulateRecognition(testData.audio.toString());
                Rule rule = await(0, deepSpeechRecognizer, speechRecognized, signal);
                assertConfidence(deepSpeechRecognizer, rule, Intention.Decide);
            }
        }
    }

}
