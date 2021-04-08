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
        super(0, HearingAbility.Good);
        this.locale = locale;
    }

    @Override
    public String languageCode() {
        return languageCode(locale);
    }

    @Override
    public void setMaxAlternates(int n) {
        // Ignore
    }

    @Override
    protected void process(SpeechRecognitionEvents events, CountDownLatch signalInitialized) {
        // noop
    }

    @Override
    public PreparedChoices prepare(Choices choices) {
        return new PreparedChoices() {

            @Override
            public void accept(SpeechRecognitionImplementation sri) {
                // Ignore
            }

            @Override
            public float hypothesisWeight(Rule rule) {
                return 0.0f;
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
