package teaselib.core.ai.deepspeech;

import static org.junit.jupiter.api.Assertions.*;
import static teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils.*;

import java.nio.file.Files;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

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

    @ParameterizedTest
    @MethodSource("tests")
    void testExpectedAudio(DeepSpeechTestData testData) throws InterruptedException {
        testAudio(testData);
    }

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
                assertConfidence(rule, Intention.Decide);
            }
        }
    }

}
