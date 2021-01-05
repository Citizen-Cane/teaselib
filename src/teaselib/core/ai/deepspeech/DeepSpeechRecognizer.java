package teaselib.core.ai.deepspeech;

import static java.util.stream.Collectors.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.IntUnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.events.EventSource;
import teaselib.core.speechrecognition.PreparedChoices;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.SpeechRecognitionNativeImplementation;
import teaselib.core.speechrecognition.SpeechRecognitionProvider;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.speechrecognition.srgs.IndexMap;
import teaselib.core.speechrecognition.srgs.PhraseString;
import teaselib.core.ui.Choices;
import teaselib.core.util.ReflectionUtils;

public class DeepSpeechRecognizer extends SpeechRecognitionNativeImplementation {
    private static final Logger logger = LoggerFactory.getLogger(DeepSpeechRecognizer.class);

    private static final Path project = ReflectionUtils.projectPath(DeepSpeechRecognizer.class);
    static final Path modelPath = project.resolve(Path.of( //
            "..", "..", "TeaseLibAIfx", "TeaseLibAIml", "models", "tflite", "deepspeech")).normalize();

    private PreparedChoicesImplementation current = null;
    private final NamedExecutorService speechEmulation = NamedExecutorService
            .singleThreadedQueue("DeepSpeech Emulation");

    public DeepSpeechRecognizer(Locale locale) {
        super(newNativeInstance(modelPath.toString(), languageCode(locale)));
    }

    protected static native long newNativeInstance(String model, String langugeCode);

    @Override
    public native String languageCode();

    enum Status {
        Idle,
        Started,
        Running,
        Cancelled,
        Done,

        ;

        public static Status of(int value) {
            return values()[value];
        }
    }

    static class Result {
        final float confidence;
        final List<String> words;

        public Result(float confidence, List<String> words) {
            this.confidence = confidence;
            this.words = words;
        }

        @Override
        public String toString() {
            return "[" + words + " confidence=" + confidence + "]";
        }

    }

    @Override
    protected void process(SpeechRecognitionEvents events, CountDownLatch signalInitialized) {
        signalInitialized.countDown();
        while (!Thread.interrupted()) {
            Status status = Status.of(decode());
            if (status == Status.Started) {
                events.recognitionStarted.fire(new SpeechRecognitionStartedEventArgs());
                fire(events.speechDetected);
            } else if (status == Status.Running) {
                fire(events.speechDetected);
            } else if (status == Status.Cancelled) {
                return;
            } else if (status == Status.Done) {
                fire(events.recognitionCompleted);
            } else if (status != Status.Idle) {
                throw new UnsupportedOperationException(status.name());
            }
        }
    }

    private void fire(EventSource<SpeechRecognizedEventArgs> event) {
        List<Result> results = results();
        if (results != null && !results.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("DeepSpeech results = \n{}",
                        results.stream().map(Objects::toString).collect(joining("\n")));
            }
            List<Rule> rules = RuleBuilder.rules(current.phrases, results);
            event.fire(new SpeechRecognizedEventArgs(rules));
        }
    }

    private final class PreparedChoicesImplementation implements PreparedChoices {
        final List<PhraseString> phrases;
        final IntUnaryOperator mapper;

        public PreparedChoicesImplementation(Choices choices) {
            IndexMap<Integer> index2choices = new IndexMap<>();
            phrases = choices.stream().flatMap(choice -> choice.phrases.stream()
                    .map(phrase -> new PhraseString(phrase.toLowerCase(), index2choices.add(choices.indexOf(choice)))))
                    .collect(toList());
            this.mapper = index2choices::get;
        }

        @Override
        public void accept(SpeechRecognitionProvider sri) {
            if (sri == DeepSpeechRecognizer.this) {
                ((DeepSpeechRecognizer) sri).current = this;
            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public float weightedProbability(Rule rule) {
            return rule.probability;
        }

        @Override
        public IntUnaryOperator mapper() {
            return mapper;
        }

    }

    @Override
    public PreparedChoices prepare(Choices choices) {
        return new PreparedChoicesImplementation(choices);
    }

    private native int decode();

    private native List<Result> results();

    @Override
    public native void startRecognition();

    @Override
    public void emulateRecognition(String speech) {
        speechEmulation.execute(() -> emulate(speech.toLowerCase()));
    }

    public native void emulate(String speech);

    @Override
    public native void stopRecognition();

    @Override
    protected native void stopEventLoop();

    @Override
    public native void dispose();

    @Override
    public void close() {
        speechEmulation.shutdown();
        while (!speechEmulation.isTerminated()) {
            try {
                speechEmulation.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        super.close();
    }

}
