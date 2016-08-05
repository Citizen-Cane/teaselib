package teaselib.core.speechrecognition;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SpeechRecognitionImplementation {
    private static final Logger logger = LoggerFactory
            .getLogger(SpeechRecognitionImplementation.class);

    /**
     * Get a recognizer for the requested language
     * 
     * @param languageCode
     *            A language code in the form XX[X]-YY[Y], like en-us, en-uk,
     *            ger, etc.
     */
    public abstract void init(
            SpeechRecognitionEvents<SpeechRecognitionImplementation> events,
            String languageCode) throws Throwable;

    public abstract void setChoices(List<String> choices);

    public abstract void setMaxAlternates(int n);

    /**
     * Start recognition using the choices in the map
     * 
     * @param choices
     *            A map containing a key, and the recognition choice as the
     *            value. The key will be returned upon a successful recognition.
     */
    public abstract void startRecognition();

    /**
     * Emulate a recognition by raising events using the emulated recognition
     * result
     * 
     * @param emulatedInput
     */
    public abstract void emulateRecognition(String emulatedRecognitionResult);

    /**
     * Stop recognition. This clears the choice map, thus a new recognition has
     * to be started afterwards
     */
    public abstract void stopRecognition();

    protected abstract void dispose();

    @Override
    protected void finalize() throws Throwable {
        try {
            dispose();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
        super.finalize();
    }
}
