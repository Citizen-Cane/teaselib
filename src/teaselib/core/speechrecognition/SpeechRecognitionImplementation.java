package teaselib.core.speechrecognition;

import java.util.Locale;

public abstract class SpeechRecognitionImplementation implements SpeechRecognitionControl, AutoCloseable {
    protected static String languageCode(final Locale locale) {
        return locale.toString().replace("_", "-");
    }

    protected abstract void dispose();

    @Override
    public abstract void close();
}
