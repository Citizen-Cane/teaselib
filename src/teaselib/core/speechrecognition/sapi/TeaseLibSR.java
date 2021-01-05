package teaselib.core.speechrecognition.sapi;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.SpeechRecognitionNativeImplementation;
import teaselib.core.speechrecognition.UnsupportedLanguageException;

abstract class TeaseLibSR extends SpeechRecognitionNativeImplementation {
    private static final Logger logger = LoggerFactory.getLogger(TeaseLibSR.class);

    public TeaseLibSR(Locale locale) {
        super(newNativeInstance(locale));
    }

    private static long newNativeInstance(Locale locale) {
        teaselib.core.jni.LibraryLoader.load("TeaseLibSR");

        String languageCode = languageCode(locale);
        try {
            return newNativeInstance(languageCode);
        } catch (UnsupportedLanguageException e) {
            if (hasRegion(languageCode)) {
                logger.warn(e.getMessage());
                languageCode = locale.getLanguage();
                logger.info("-> trying language {}", languageCode);
                return newNativeInstance(languageCode);
            } else {
                throw e;
            }
        }
    }

    /**
     * Create a native recognizer instance for the requested language.
     * 
     * @param languageCode
     *            A language code in the form XX[X]-YY[Y], like en-us, en-uk, ger, etc.
     * @return Native object or 0 if the creation failed due to mising language support.
     * @throws UnsupportedLanguageException
     *             When the requested language is not supported.
     */
    protected static native long newNativeInstance(String langugeCode);

    /**
     * Init the speech recognizer event handling thread. Creating the thread in native code and then attaching the VM
     * would cause the thread to get a default class loader, without the class path to teaselib, resulting in not being
     * able to create event objects
     */
    @Override
    protected native void process(SpeechRecognitionEvents events, CountDownLatch signalInitialized);

    protected abstract List<Rule> repair(List<Rule> result);

    @Override
    public native String languageCode();

    public native void setChoices(List<String> phrases);

    public native void setChoices(byte[] srgs);

    native void setMaxAlternates(int n);

    @Override
    public native void startRecognition();

    @Override
    public native void emulateRecognition(String emulatedRecognitionResult);

    @Override
    public native void stopRecognition();

    @Override
    protected native void stopEventLoop();

    @Override
    public native void dispose();

}
