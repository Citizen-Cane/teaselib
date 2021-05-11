package teaselib.core.speechrecognition.sapi;

import static teaselib.core.jni.NativeLibraries.TEASELIB;
import static teaselib.core.jni.NativeLibraries.TEASELIB_SR;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.jni.NativeLibraries;
import teaselib.core.speechrecognition.HearingAbility;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.SpeechRecognitionNativeImplementation;
import teaselib.core.speechrecognition.SpeechRecognitionTimeoutWatchdog;
import teaselib.core.speechrecognition.UnsupportedLanguageException;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;

abstract class TeaseLibSR extends SpeechRecognitionNativeImplementation {
    private static final Logger logger = LoggerFactory.getLogger(TeaseLibSR.class);

    protected TeaseLibSR(Locale locale) {
        super(newNativeInstance(locale), HearingAbility.Impaired);
    }

    private static long newNativeInstance(Locale locale) {
        NativeLibraries.require(TEASELIB, TEASELIB_SR);
        return newNativeInstance(locale, TeaseLibSR::newNativeInstance);
    }

    /**
     * Create a native recognizer instance for the requested language.
     * 
     * @param languageCode
     *            A language code in the form XX[X][-YY[Y]], like en-us, en-uk, en, de-ger, de-de, de, etc.
     * @return Native object or 0 if the creation failed due to missing language support.
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

    /**
     * Called by the native code to repair inconsistent SAPI rules.
     * 
     * @param result
     *            The result of a speech detection.
     * @return The same result, or a new list with some rules repaired.
     */
    protected abstract List<Rule> repair(List<Rule> result);

    @Override
    public native String languageCode();

    @Override
    public native void setMaxAlternates(int n);

    public native void setChoices(List<String> phrases);

    public native void setChoices(byte[] srgs);

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

    public abstract static class SAPI extends TeaseLibSR {

        private SpeechRecognitionTimeoutWatchdog timeoutWatchdog;

        protected SAPI(Locale locale) {
            super(locale);
        }

        @Override
        public void startEventLoop(SpeechRecognitionEvents events) {
            this.timeoutWatchdog = new SpeechRecognitionTimeoutWatchdog(events, this::handleRecognitionTimeout);
            this.timeoutWatchdog.addEvents();
            super.startEventLoop(events);
        }

        private void handleRecognitionTimeout(SpeechRecognitionEvents events) {
            events.recognitionRejected.fire(new SpeechRecognizedEventArgs(Rule.Timeout));
        }

        @Override
        public void startRecognition() {
            timeoutWatchdog.enable(true);
            super.startRecognition();
        }

        @Override
        public void stopRecognition() {
            super.stopRecognition();
            timeoutWatchdog.enable(false);
        }

        @Override
        protected void stopEventLoop() {
            super.stopEventLoop();
            timeoutWatchdog.enable(false);
            timeoutWatchdog.removeEvents();
        }

    }

}
