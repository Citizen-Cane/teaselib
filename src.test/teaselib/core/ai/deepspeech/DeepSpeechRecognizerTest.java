package teaselib.core.ai.deepspeech;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.jupiter.api.Assertions.*;
import static teaselib.core.ai.deepspeech.DeepSpeechTestData.*;
import static teaselib.core.util.ExceptionUtil.*;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import teaselib.core.ai.TeaseLibAI;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;

public class DeepSpeechRecognizerTest {

    @Test
    public void testExperienceProovesThis() throws InterruptedException {
        testAudio(AUDIO_2830_3980_0043_RAW);
    }

    @Test
    public void testWhyShouldOneHaltOnTheWay() throws InterruptedException {
        testAudio(AUDIO_4507_16021_0012_RAW);
    }

    @Test
    public void testYourPowerIsSufficientIsaid() throws InterruptedException {
        testAudio(AUDIO_8455_210777_0068_RAW);
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
                deepSpeechRecognizer.emulateRecognition(testData.audio.toString());
                await(deepSpeechRecognizer, speechRecognized, signal, testData.actual);
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
                deepSpeechRecognizer.emulateRecognition(
                        "experience prooves this\nexperience prooves that\nthe experience proofs it");
                await(deepSpeechRecognizer, speechRecognized, signal, "experience prooves this");
                assertNotNull(speechRecognized);
            }
        }
    }

    private static void await(DeepSpeechRecognizer deepSpeechRecognizer,
            AtomicReference<SpeechRecognizedEventArgs> event, CountDownLatch signal, String speech)
            throws InterruptedException {
        if (signal.await(10, SECONDS)) {
            List<Rule> result = event.get().result;
            assertFalse(result.isEmpty());
            // TODO re-construct expected from hypotheses and assert confidence
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
