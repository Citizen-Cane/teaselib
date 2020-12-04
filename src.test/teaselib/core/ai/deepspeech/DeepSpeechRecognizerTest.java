package teaselib.core.ai.deepspeech;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
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
import teaselib.core.util.ReflectionUtils;

public class DeepSpeechRecognizerTest {

    private static final Path project = ReflectionUtils.projectPath(DeepSpeechRecognizer.class);
    private static final Path model = project.resolve(Path.of( //
            "..", "..", "TeaseLibAIfx", "TeaseLibAIml", "models", "tflite", "deepspeech")).normalize();
    private static final Path AUDIO_2830_3980_0043_RAW = model
            .resolve(Path.of(Locale.ENGLISH.getLanguage(), "audio/2830-3980-0043.raw"));
    private static final Path AUDIO_4507_16021_0012_RAW = model
            .resolve(Path.of(Locale.ENGLISH.getLanguage(), "audio/4507-16021-0012.raw"));
    private static final Path AUDIO_8455_210777_0068_RAW = model
            .resolve(Path.of(Locale.ENGLISH.getLanguage(), "audio/8455-210777-0068.raw"));

    @Test
    public void testExperienceProovesThis() throws InterruptedException {
        testAudio(AUDIO_2830_3980_0043_RAW, "experience prooves this", "experience proves this");
    }

    @Test
    public void testWhyShouldOneHaltOnTheWay() throws InterruptedException {
        testAudio(AUDIO_4507_16021_0012_RAW, "why should one halt on the way", "why should one halt on the way");
    }

    @Test
    public void testYourPowerIsSufficientIsaid() throws InterruptedException {
        testAudio(AUDIO_8455_210777_0068_RAW, "your power is sufficient i said", "your part is sufficient i said");
    }

    public void testAudio(Path audioFile, String groundTruth, String expected) throws InterruptedException {
        try (TeaseLibAI teaseLibAI = new TeaseLibAI()) {
            SpeechRecognitionEvents events = new SpeechRecognitionEvents();

            AtomicReference<SpeechRecognizedEventArgs> speechRecognized = new AtomicReference<>();
            CountDownLatch signal = new CountDownLatch(1);
            events.recognitionCompleted.add(e -> {
                speechRecognized.set(e);
                signal.countDown();
            });
            try (DeepSpeechRecognizer deepSpeechRecognizer = new DeepSpeechRecognizer(model, Locale.ENGLISH, events)) {
                deepSpeechRecognizer.emulateRecognition(audioFile.toString());
                await(deepSpeechRecognizer, speechRecognized, signal, expected);
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
            try (DeepSpeechRecognizer deepSpeechRecognizer = new DeepSpeechRecognizer(model, Locale.ENGLISH, events)) {
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
