package teaselib.core.speechrecognition;

import java.util.Locale;

import teaselib.core.Closeable;

public abstract class SpeechRecognitionImplementation implements SpeechRecognitionControl, Closeable {
    protected String languageCode;

    protected static String languageCode(Locale locale) {
        return locale.toString().replace("_", "-");
    }

    protected abstract void dispose();

    @Override
    public abstract void close();

    public String getLanguageCode() {
        return languageCode;
    }
}
