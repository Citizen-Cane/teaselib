package teaselib.core.speechrecognition;

import java.util.Locale;

import teaselib.core.Closeable;

public abstract class SpeechRecognitionImplementation implements SpeechRecognitionControl, Closeable {
    protected String languageCode;

    interface Setup<T extends SpeechRecognitionImplementation> {
        void applyTo(T sr);
    }

    protected static String languageCode(Locale locale) {
        return locale.toString().replace("_", "-");
    }

    protected abstract void dispose();

    public String getLanguageCode() {
        return languageCode;
    }
}
