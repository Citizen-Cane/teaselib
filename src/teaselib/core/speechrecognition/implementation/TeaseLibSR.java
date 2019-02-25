package teaselib.core.speechrecognition.implementation;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.speechrecognition.SpeechRecognitionChoices;
import teaselib.core.speechrecognition.SpeechRecognitionControl;
import teaselib.core.speechrecognition.SpeechRecognitionEvents;
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;

public class TeaseLibSR extends SpeechRecognitionImplementation
        implements /* SpeechRecognitionSRGS, */ SpeechRecognitionChoices {
    private static final Logger logger = LoggerFactory.getLogger(TeaseLibSR.class);

    private long nativeObject;

    private Thread eventThread = null;
    private Throwable eventThreadException = null;

    public TeaseLibSR() throws UnsatisfiedLinkError {
        teaselib.core.jni.LibraryLoader.load("TeaseLibSR");
    }

    @Override
    public void init(final SpeechRecognitionEvents<SpeechRecognitionControl> events, Locale locale) throws Throwable {
        CountDownLatch awaitStart = new CountDownLatch(1);

        Runnable speechRecognitionService = () -> {
            try {
                String languageCode = languageCode(locale);
                initSR(events, languageCode);
                awaitStart.countDown();
                // Never ends
                initSREventThread(eventThread);
            } catch (Exception e) {
                logger.warn(e.getMessage());
                logger.info("-> trying language {}", locale.getLanguage());
                initSR(events, locale.getLanguage());
                try {
                    initSREventThread(eventThread);
                } catch (Exception e1) {
                    logger.error(e.getMessage(), e);
                    eventThreadException = e;
                }
            } finally {
                awaitStart.countDown();
            }
        };

        eventThread = new Thread(speechRecognitionService);
        synchronized (eventThread) {
            eventThread.setName("Speech Recognition event thread");
            eventThread.start();
            awaitStart.await();
            if (eventThreadException != null) {
                throw eventThreadException;
            }
        }
    }

    /**
     * // Init the recognizer
     * 
     * @param languageCode
     */
    public native void initSR(SpeechRecognitionEvents<SpeechRecognitionControl> events, String languageCode);

    /**
     * Init the speech recognizer event handling thread. Creating the thread in native code and then attaching the VM
     * would cause the thread to get a default class loader, without the class path to teaselib, resulting in not being
     * able to create event objects
     */
    private native void initSREventThread(Thread jthread);

    @Override
    public native void setChoices(List<String> choices);

    /* @Override */
    public native void setChoices(String srgs);

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
    protected void finalize() throws Throwable {
        try {
            dispose();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
        super.finalize();
    }
}
