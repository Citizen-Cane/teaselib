package teaselib.core.speechrecognition;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SpeechRecognitionImplementation implements SpeechRecognitionControl {
    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionImplementation.class);

    /*
     * (non-Javadoc)
     * 
     * @see teaselib.core.speechrecognition.SpeechRecognitionControl#init(teaselib.core.speechrecognition.
     * SpeechRecognitionEvents, java.util.Locale)
     */
    @Override
    public abstract void init(SpeechRecognitionEvents<SpeechRecognitionControl> events, Locale locale) throws Throwable;

    /*
     * (non-Javadoc)
     * 
     * @see teaselib.core.speechrecognition.SpeechRecognitionControl#setMaxAlternates(int)
     */
    @Override
    public abstract void setMaxAlternates(int n);

    /*
     * (non-Javadoc)
     * 
     * @see teaselib.core.speechrecognition.SpeechRecognitionControl#startRecognition()
     */
    @Override
    public abstract void startRecognition();

    /*
     * (non-Javadoc)
     * 
     * @see teaselib.core.speechrecognition.SpeechRecognitionControl#emulateRecognition(java.lang.String)
     */
    @Override
    public abstract void emulateRecognition(String emulatedRecognitionResult);

    /*
     * (non-Javadoc)
     * 
     * @see teaselib.core.speechrecognition.SpeechRecognitionControl#stopRecognition()
     */
    @Override
    public abstract void stopRecognition();

    protected abstract void dispose();

    protected static String languageCode(final Locale locale) {
        return locale.toString().replace("_", "-");
    }

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
