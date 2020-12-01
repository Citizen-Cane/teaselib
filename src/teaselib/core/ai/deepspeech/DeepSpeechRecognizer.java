package teaselib.core.ai.deepspeech;

import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import teaselib.core.speechrecognition.PreparedChoices;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.SpeechRecognitionNativeImplementation;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.ui.Choices;

public class DeepSpeechRecognizer extends SpeechRecognitionNativeImplementation {

    public DeepSpeechRecognizer(Locale locale, SpeechRecognitionEvents events) {
        super(init(languageCode(locale)), events);
    }

    protected static native long init(String langugeCode);

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

    class Result {
        final float confidence;
        final List<String> words;

        public Result(float confidence, List<String> words) {
            super();
            this.confidence = confidence;
            this.words = words;
        }
    }

    @Override
    protected void process(SpeechRecognitionEvents events, CountDownLatch signalInitialized) {
        signalInitialized.countDown();

        while (!Thread.interrupted()) {
            Status status = Status.of(decode());
            if (status == Status.Idle) {
                // Ignore
            } else if (status == Status.Started) {
                events.speechDetected.fire(new SpeechRecognizedEventArgs(rules(results())));
            } else if (status == Status.Running) {
                events.speechDetected.fire(new SpeechRecognizedEventArgs(rules(results())));
            } else if (status == Status.Cancelled) {
                Thread.currentThread().interrupt();
            } else if (status == Status.Done) {
                events.speechDetected.fire(new SpeechRecognizedEventArgs(rules(results())));
            } else {
                throw new UnsupportedOperationException(status.name());
            }
        }
    }

    private List<Rule> rules(List<Result> results) {
        return results.stream().map(r -> new Rule(Rule.MAIN_RULE_NAME, r.words, 0, r.confidence))
                .collect(toUnmodifiableList());
    }

    @Override
    public PreparedChoices prepare(Choices choices) {
        throw new UnsupportedOperationException();
    }

    private native int decode();

    private native List<Result> results();

    @Override
    public native void startRecognition();

    @Override
    public native void emulateRecognition(String emulatedRecognitionResult);

    @Override
    public native void stopRecognition();

    @Override
    public native void dispose();

}
