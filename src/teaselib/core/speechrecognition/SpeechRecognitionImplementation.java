package teaselib.core.speechrecognition;

import teaselib.core.ui.Choices;

public interface SpeechRecognitionImplementation {

    static final int MAX_ALTERNATES_DEFAULT = 5;

    /**
     * The language code of the recognizer.
     * 
     * @return Language code in the form "lang"-"region", for instance "en-us".
     * 
     */
    String languageCode();

    /**
     * Set the maximal number of alternate recognition results
     * 
     * @param n
     *            Number of alternate results
     */
    void setMaxAlternates(int n);

    /**
     * Create a recognition parameters object to set the choices for this Speech Recognition implementation. Since the
     * preparations may take some time, the object may be created in a worker thread or prior to starting recognition.
     * May be applied multiple times.
     * 
     * @param choices
     *            Choices to prepare.
     * @return Setter to apply a set of choices.
     */
    PreparedChoices prepare(Choices choices);

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
