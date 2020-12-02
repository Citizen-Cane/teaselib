package teaselib.core.speechrecognition;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import teaselib.core.Closeable;
import teaselib.core.jni.NativeObject;
import teaselib.core.util.ExceptionUtil;

public abstract class SpeechRecognitionNativeImplementation extends NativeObject
        implements Closeable, SpeechRecognitionProvider {

    private Thread eventThread = null;
    private Throwable eventThreadException = null;

    public SpeechRecognitionNativeImplementation(long nativeObject, SpeechRecognitionEvents events) {
        super(nativeObject);
        process(events);
    }

    protected static String languageCode(Locale locale) {
        return locale.toString().replace("_", "-");
    }

    public final void process(SpeechRecognitionEvents events) {
        CountDownLatch awaitInitialized = new CountDownLatch(1);
        Runnable speechRecognitionService = () -> {
            try {
                process(events, awaitInitialized);
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
        try {
            awaitInitialized.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (eventThreadException != null) {
            throw ExceptionUtil.asRuntimeException(eventThreadException);
        }
    }

    /**
     * Run the event thread until disposed.
     * 
     * @param events
     * @param signalInitialized
     */
    protected abstract void process(SpeechRecognitionEvents events, CountDownLatch signalInitialized);

    /**
     * Stop the event thread and clean up.
     */
    @Override
    protected abstract void dispose();

    @Override
    public void close() {
        super.close();
        eventThread.interrupt();
        try {
            eventThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
