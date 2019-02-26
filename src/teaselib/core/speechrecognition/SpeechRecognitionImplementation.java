package teaselib.core.speechrecognition;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SpeechRecognitionImplementation implements SpeechRecognitionControl {
    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionImplementation.class);

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
