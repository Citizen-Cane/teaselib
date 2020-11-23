package teaselib.core.speechrecognition;

import java.util.Locale;
import java.util.function.IntUnaryOperator;

import teaselib.core.ui.Choices;

/**
 * @author Citizen-Cane
 *
 */
class Unsupported extends SpeechRecognitionImplementation {

    public static final Unsupported Instance = new Unsupported();

    @Override
    public void init(SpeechRecognitionEvents events, Locale locale) {
        // Ignore
    }

    @Override
    public PreparedChoices prepare(Choices choices) {
        return new PreparedChoices() {

            @Override
            public void accept(SpeechRecognitionImplementation sri) {
                // Ignore
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
    protected void dispose() {
        // Ignore
    }

    @Override
    public void close() {
        // Ignore
    }

}
