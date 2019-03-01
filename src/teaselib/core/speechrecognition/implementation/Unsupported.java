package teaselib.core.speechrecognition.implementation;

import java.util.Locale;

import teaselib.core.speechrecognition.SpeechRecognitionControl;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;

/**
 * @author Citizen-Cane
 *
 */
public class Unsupported extends SpeechRecognitionImplementation {
    public static final Unsupported Instance = new Unsupported();

    @Override
    public void init(SpeechRecognitionEvents<SpeechRecognitionControl> events, Locale locale) throws Throwable {
        // Ignore
    }

    @Override
    public void setMaxAlternates(int n) {
        // Ignore
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
