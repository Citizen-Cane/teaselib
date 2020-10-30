package teaselib.core.speechrecognition;

import java.util.Locale;

import teaselib.core.Closeable;
import teaselib.core.ui.Choices;

public abstract class SpeechRecognitionImplementation implements Closeable {
    protected String languageCode;

    protected static String getLanguageCode(Locale locale) {
        return locale.toString().replace("_", "-");
    }

    protected abstract void dispose();

    public String getLanguageCode() {
        return languageCode;
    }

    /**
     * Get a recognizer for the requested language
     * 
     * @param languageCode
     *            A language code in the form XX[X]-YY[Y], like en-us, en-uk, ger, etc.
     */
    public abstract void init(SpeechRecognitionEvents events, Locale locale);

    /**
     * Create a recognition parameters object to set the choices for this Speech Recognition implementation. Since the
     * preparations may take some time, the object may be created in a worker thread or prior to starting recognition.
     * May be applied multiple times.
     * 
     * @param choices
     *            Choices to prepare.
     * @return Setter to apply a set of choices.
     */
    public abstract PreparedChoices prepare(Choices choices);

    /**
     * Start recognition using the choices in the map
     * 
     * @param choices
     *            A map containing a key, and the recognition choice as the value. The key will be returned upon a
     *            successful recognition.
     */
    public abstract void startRecognition();

    /**
     * Emulate a recognition by raising events using the emulated recognition result
     * 
     * @param emulatedInput
     */
    public abstract void emulateRecognition(String emulatedRecognitionResult);

    /**
     * Stop recognition. This clears the choice map, thus a new recognition has to be started afterwards
     */
    public abstract void stopRecognition();

    @Override
    public abstract void close();

}
