package teaselib.core.speechrecognition;

import java.util.Locale;

public interface SpeechRecognitionControl {

    /**
     * Get a recognizer for the requested language
     * 
     * @param languageCode
     *            A language code in the form XX[X]-YY[Y], like en-us, en-uk, ger, etc.
     */
    void init(SpeechRecognitionEvents<SpeechRecognitionControl> events, Locale locale) throws Throwable;

    void setMaxAlternates(int n);

    /**
     * Start recognition using the choices in the map
     * 
     * @param choices
     *            A map containing a key, and the recognition choice as the value. The key will be returned upon a
     *            successful recognition.
     */
    void startRecognition();

    /**
     * Emulate a recognition by raising events using the emulated recognition result
     * 
     * @param emulatedInput
     */
    void emulateRecognition(String emulatedRecognitionResult);

    /**
     * Stop recognition. This clears the choice map, thus a new recognition has to be started afterwards
     */
    void stopRecognition();

}