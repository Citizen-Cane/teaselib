package teaselib.core.speechrecognition;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.function.IntUnaryOperator;

import teaselib.core.ui.Choices;

/**
 * @author Citizen-Cane
 *
 */
class Unsupported extends SpeechRecognitionNativeImplementation {

    public static final Unsupported Instance = new Unsupported(Locale.getDefault());

    private final Locale locale;

    public Unsupported(Locale locale) {
        super(0);
        this.locale = locale;
    }

    @Override
    public String languageCode() {
        return languageCode(locale);
    }

    @Override
    protected void process(SpeechRecognitionEvents events, CountDownLatch signalInitialized) {
        // noop
    }

    @Override
    public PreparedChoices prepare(Choices choices) {
        return new PreparedChoices() {

            @Override
            public void accept(SpeechRecognitionProvider sri) {
                // Ignore
            }

            @Override
            public float weightedProbability(Rule rule) {
                return rule.probability;
            }

            @Override
            public IntUnaryOperator mapper() {
                return IdentityMapping;
            }
        };
    }

    @Override
    public void startRecognition() {
        // Ignore
    }

    @Override
    public void emulateRecognition(String emulatedRecognitionResult) {
        // Ignore
    }

    @Override
    public void stopRecognition() {
        // Ignore
    }

    @Override
    protected void stopEventLoop() {
        // noop
    }

    @Override
    protected void dispose() {
        // noop
    }

    @Override
    public void close() {
        // Ignore
    }

}
