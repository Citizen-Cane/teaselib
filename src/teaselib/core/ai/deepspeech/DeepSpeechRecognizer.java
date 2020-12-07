package teaselib.core.ai.deepspeech;

import static java.util.stream.Collectors.toUnmodifiableList;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;

import teaselib.core.speechrecognition.PreparedChoices;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.SpeechRecognitionNativeImplementation;
import teaselib.core.speechrecognition.SpeechRecognitionProvider;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.speechrecognition.srgs.IndexMap;
import teaselib.core.speechrecognition.srgs.PhraseString;
import teaselib.core.speechrecognition.srgs.PhraseStringSequences;
import teaselib.core.speechrecognition.srgs.SlicedPhrases;
import teaselib.core.ui.Choices;

public class DeepSpeechRecognizer extends SpeechRecognitionNativeImplementation {

    private PreparedChoicesImplementation current = null;

    public DeepSpeechRecognizer(Path model, Locale locale, SpeechRecognitionEvents events) {
        super(init(model.toString(), languageCode(locale)), events);
    }

    protected static native long init(String model, String langugeCode);

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
    }

    @Override
    protected void process(SpeechRecognitionEvents events, CountDownLatch signalInitialized) {
        signalInitialized.countDown();
        while (!Thread.interrupted()) {
            Status status = Status.of(decode());
            if (status == Status.Started) {
                events.recognitionStarted.fire(new SpeechRecognitionStartedEventArgs());
                events.speechDetected.fire(new SpeechRecognizedEventArgs(rules(results())));
            } else if (status == Status.Running) {
                events.speechDetected.fire(new SpeechRecognizedEventArgs(rules(results())));
            } else if (status == Status.Cancelled) {
                Thread.currentThread().interrupt();
            } else if (status == Status.Done) {
                events.recognitionCompleted.fire(new SpeechRecognizedEventArgs(rules(results())));
            } else if (status != Status.Idle) {
                throw new UnsupportedOperationException(status.name());
            }
        }
    }

    private List<Rule> rules(List<Result> results) {
        return results.stream().map(r -> new Rule(Rule.MAIN_RULE_NAME, r.words, 0, r.confidence))
                .collect(toUnmodifiableList());
    }

    private final class PreparedChoicesImplementation implements PreparedChoices {

        final Choices choices;

        final SlicedPhrases<PhraseString> slices;
        final IntUnaryOperator mapper;

        public PreparedChoicesImplementation(Choices choices) {
            this.choices = choices;

            IndexMap<Integer> index2choices = new IndexMap<>();
            List<PhraseString> phrases = choices.stream()
                    .flatMap(choice -> choice.phrases.stream()
                            .map(phrase -> new PhraseString(phrase, index2choices.add(choices.indexOf(choice)))))
                    .collect(Collectors.toList());

            this.slices = SlicedPhrases.of( //
                    PhraseStringSequences.of(phrases), PhraseStringSequences::prettyPrint);
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
        Thread thread = new Thread(() -> emulate(speech), "DeepSpeech input emulation");
        thread.setDaemon(true);
        thread.start();
    }

    public native void emulate(String speech);

    @Override
    public native void stopRecognition();

    @Override
    protected native void stopEventLoop();

    @Override
    public native void dispose();

}
