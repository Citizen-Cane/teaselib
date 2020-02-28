package teaselib.core.speechrecognition.implementation;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.speechrecognition.SpeechRecognitionChoices;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;

public class TeaseLibSR extends SpeechRecognitionImplementation implements SpeechRecognitionChoices {
    private static final Logger logger = LoggerFactory.getLogger(TeaseLibSR.class);

    private long nativeObject;

    private Thread eventThread = null;
    private Throwable eventThreadException = null;

    public TeaseLibSR() throws UnsatisfiedLinkError {
        teaselib.core.jni.LibraryLoader.load("TeaseLibSR");
    }

    @Override
    public void init(SpeechRecognitionEvents events, Locale locale) throws Throwable {
        languageCode = languageCode(locale);
        try {
            initSR(getLanguageCode());
        } catch (UnsupportedLanguageException e) {
            logger.warn(e.getMessage());
            int index = getLanguageCode().indexOf("-");
            if (index < 0) {
                throw e;
            } else {
                languageCode = getLanguageCode().substring(0, index);
                logger.info("-> trying language {}", getLanguageCode());
                initSR(getLanguageCode());
            }
        }

        CountDownLatch awaitInitialized = new CountDownLatch(1);
        Runnable speechRecognitionService = () -> {
            try {
                initSREventThread(events, awaitInitialized);
            } catch (Exception e) {
                eventThreadException = e;
            } catch (Throwable t) {
                eventThreadException = t;
            } finally {
                awaitInitialized.countDown();
            }
        };

        eventThread = new Thread(speechRecognitionService);
        eventThread.setName("Speech Recognition event thread");
        eventThread.start();
        awaitInitialized.await();
        if (eventThreadException != null) {
            throw eventThreadException;
        }
    }

    /**
     * // Init the recognizer
     * 
     * @param languageCode
     */
    public native void initSR(String languageCode);

    /**
     * Init the speech recognizer event handling thread. Creating the thread in native code and then attaching the VM
     * would cause the thread to get a default class loader, without the class path to teaselib, resulting in not being
     * able to create event objects
     */
    private native void initSREventThread(SpeechRecognitionEvents events, CountDownLatch signalInitialized);

    @Override
    public native void setChoices(List<String> phrases);

    /* @Override */
    public native void setChoices(byte[] srgs);

    @Override
    public native void setMaxAlternates(int n);

    @Override
    public native void startRecognition();

    @Override
    public native void emulateRecognition(String emulatedRecognitionResult);

    @Override
    public native void stopRecognition();

    @Override
    public native void dispose();

    @Override
    public void close() {
        dispose();
        try {
            eventThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
