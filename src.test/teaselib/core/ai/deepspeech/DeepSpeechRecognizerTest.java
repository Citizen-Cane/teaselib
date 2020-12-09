package teaselib.core.ai.deepspeech;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static teaselib.core.ai.deepspeech.DeepSpeechTestData.AUDIO_2830_3980_0043_RAW;
import static teaselib.core.ai.deepspeech.DeepSpeechTestData.AUDIO_4507_16021_0012_RAW;
import static teaselib.core.ai.deepspeech.DeepSpeechTestData.AUDIO_8455_210777_0068_RAW;

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
import teaselib.core.util.ExceptionUtil;

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

    public void testAudio(DeepSpeechTestData testData) throws InterruptedException {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            SpeechRecognitionEvents events = new SpeechRecognitionEvents();

            AtomicReference<SpeechRecognizedEventArgs> speechRecognized = new AtomicReference<>();
            CountDownLatch signal = new CountDownLatch(1);
            events.recognitionCompleted.add(e -> {
                speechRecognized.set(e);
                signal.countDown();
            });
            try (DeepSpeechRecognizer deepSpeechRecognizer = new DeepSpeechRecognizer(Locale.ENGLISH, events)) {
                deepSpeechRecognizer.emulateRecognition(testData.audio.toString());
                await(deepSpeechRecognizer, speechRecognized, signal, testData.actual);
                // TODO test expected and assert confidence
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
            try (DeepSpeechRecognizer deepSpeechRecognizer = new DeepSpeechRecognizer(Locale.ENGLISH, events)) {
                deepSpeechRecognizer.emulateRecognition(
                        "experience prooves this\nexperience prooves that\nthe experience proofs it");
                await(deepSpeechRecognizer, speechRecognized, signal, "experience prooves this");
            }
        }
    }

    private void await(DeepSpeechRecognizer deepSpeechRecognizer, AtomicReference<SpeechRecognizedEventArgs> event,
            CountDownLatch signal, String speech) throws InterruptedException {
        if (signal.await(10, SECONDS)) {
            List<Rule> result = event.get().result;
            assertFalse(result.isEmpty());
            assertEquals(speech, result.get(0).text);
        } else {
            Optional<Throwable> failure = deepSpeechRecognizer.getException();
            if (failure.isPresent()) {
                throw ExceptionUtil.asRuntimeException(failure.get());
            } else {
                fail("Speech detection timed out");
            }
        }
    }

}
