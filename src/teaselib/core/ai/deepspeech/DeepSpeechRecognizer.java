package teaselib.core.ai.deepspeech;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.IntUnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.speechrecognition.AudioSignalProblem;
import teaselib.core.speechrecognition.PreparedChoices;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.SpeechRecognitionNativeImplementation;
import teaselib.core.speechrecognition.SpeechRecognitionProvider;
import teaselib.core.speechrecognition.events.AudioSignalProblemOccuredEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.speechrecognition.srgs.PhraseString;
import teaselib.core.ui.Choices;

public class DeepSpeechRecognizer extends SpeechRecognitionNativeImplementation {
    private static final AudioSignalProblemOccuredEventArgs NoiseDetected = new AudioSignalProblemOccuredEventArgs(
            AudioSignalProblem.Noise);

    private static final SpeechRecognizedEventArgs TimeoutEvent = new SpeechRecognizedEventArgs(
            Collections.singletonList(Rule.Timeout));

    private static final Logger logger = LoggerFactory.getLogger(DeepSpeechRecognizer.class);

    private PreparedChoicesImplementation current = null;
    private final NamedExecutorService speechEmulation = NamedExecutorService
            .singleThreadedQueue("DeepSpeech Emulation");

    public DeepSpeechRecognizer(Locale locale) {
        super(newNativeInstance(locale, DeepSpeechRecognizer::newNativeInstance));
    }

    protected static native long newNativeInstance(String langugeCode);

    @Override
    public native String languageCode();

    enum Status {
        Idle,
        Started,
        Running,
        Noise,
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

    private Status recognitionState = Status.Idle;

    @Override
    protected void process(SpeechRecognitionEvents events, CountDownLatch signalInitialized) {
        signalInitialized.countDown();
        while (!Thread.interrupted()) {
            Status status = Status.of(decode());
            try {
                if (status == Status.Started) {
                    List<Rule> rules = rules();
                    if (!rules.isEmpty()) {
                        startRecognition(events);
                        fireSpeechDetected(events);
                    } else {
                        recognitionState = Status.Idle;
                    }
                } else if (status == Status.Running) {
                    if (recognitionState == Status.Idle) {
                        startRecognition(events);
                    }
                    fireSpeechDetected(events);
                } else if (status == Status.Noise) {
                    events.audioSignalProblemOccured.fire(NoiseDetected);
                    rejectRecognition(events);
                } else if (status == Status.Cancelled) {
                    rejectRecognition(events);
                    return;
                } else if (status == Status.Done) {
                    recognitionState = Status.Idle;
                    fireRecognitionDone(events);
                } else if (status == Status.Idle) {
                    rejectRecognition(events);
                } else {
                    throw new UnsupportedOperationException(status.name());
                }
            } catch (Throwable t) {
                stopEventLoop();
                freeDeepSpeechStreamOnError(status);
                throw t;
            }
        }

    }

    private void startRecognition(SpeechRecognitionEvents events) {
        recognitionState = Status.Running;
        events.recognitionStarted.fire(new SpeechRecognitionStartedEventArgs());
    }

    private void rejectRecognition(SpeechRecognitionEvents events) {
        if (recognitionState != Status.Idle) {
            recognitionState = Status.Idle;
            fireRecognitionRejected(events);
        }
    }

    private void fireRecognitionRejected(SpeechRecognitionEvents events) {
        List<Rule> rules = rules();
        if (rules.isEmpty()) {
            events.recognitionRejected.fire(TimeoutEvent);
        } else {
            events.recognitionRejected.fire(new SpeechRecognizedEventArgs(rules));
        }
    }

    private void fireRecognitionDone(SpeechRecognitionEvents events) {
        List<Rule> rules = rules();
        if (rules.isEmpty()) {
            events.recognitionRejected.fire(TimeoutEvent);
        } else {
            events.recognitionCompleted.fire(new SpeechRecognizedEventArgs(rules));
        }

    }

    private void freeDeepSpeechStreamOnError(Status status) {
        if (status == Status.Running) {
            decode();
        }
    }

    private void fireSpeechDetected(SpeechRecognitionEvents events) {
        List<Rule> rules = rules();
        if (!rules.isEmpty()) {
            events.speechDetected.fire(new SpeechRecognizedEventArgs(rules));
        }
    }

    private List<Rule> rules() {
        List<Rule> rules;
        List<Result> results = results();
        if (results == null || results.isEmpty()) {
            rules = Collections.emptyList();
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("DeepSpeech results = \n{}",
                        results.stream().map(Objects::toString).collect(joining("\n")));
            }
            rules = RuleBuilder.rules(current.phrases, results);
        }
        return rules;
    }

    private final class PreparedChoicesImplementation implements PreparedChoices {
        final List<String[]> phrases;
        final IntUnaryOperator mapper;

        public PreparedChoicesImplementation(Choices choices) {
            phrases = choices.stream()
                    .flatMap(choice -> choice.phrases.stream().map(String::toLowerCase).map(PhraseString::words))
                    .collect(toList());
            this.mapper = choices.indexMapper();
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
