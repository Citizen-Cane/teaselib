package teaselib.core.speechrecognition;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.ToLongFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Closeable;
import teaselib.core.jni.NativeObject;
import teaselib.core.util.ExceptionUtil;

/**
 * @author Citizen-Cane
 *
 */
public abstract class SpeechRecognitionNativeImplementation extends NativeObject
        implements Closeable, SpeechRecognitionProvider {
    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionNativeImplementation.class);

    private final class EventLoopThread extends Thread {

        private EventLoopThread(Runnable target) {
            super(target);
        }

        @Override
        public void interrupt() {
            stopEventLoop();
        }

    }

    private EventLoopThread eventLoopThread = null;
    private Throwable eventLoopException = null;

    public SpeechRecognitionNativeImplementation(long nativeObject) {
        super(nativeObject);
    }

    protected static long newNativeInstance(Locale locale, ToLongFunction<String> newNativeInstance) {
        String languageCode = languageCode(locale);
        try {
            return newNativeInstance.applyAsLong(languageCode);
        } catch (UnsupportedLanguageException e) {
            if (hasRegion(languageCode)) {
                logger.warn(e.getMessage());
                languageCode = locale.getLanguage();
                logger.info("-> trying language {}", languageCode);
                return newNativeInstance.applyAsLong(languageCode);
            } else {
                throw e;
            }
        }
    }

    public static String languageCode(Locale locale) {
        return locale.toString().replace("_", "-");
    }

    public static boolean hasRegion(String languageCode) {
        int index = languageCode.indexOf("-");
        return index >= 0;
    }

    public void startEventLoop(SpeechRecognitionEvents events) {
        CountDownLatch awaitInitialized = new CountDownLatch(1);
        Runnable speechRecognitionService = () -> {
            try {
                process(events, awaitInitialized);
            } catch (Exception e) {
                eventLoopException = e;
            } catch (Throwable t) {
                eventLoopException = t;
            } finally {
                awaitInitialized.countDown();
            }
        };

        eventLoopThread = new EventLoopThread(speechRecognitionService);
        eventLoopThread.setName("Speech Recognition event thread");
        eventLoopThread.start();
        try {
            awaitInitialized.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (eventLoopException != null) {
            throw ExceptionUtil.asRuntimeException(eventLoopException);
        }
    }

    /**
     * Run the event thread until disposed.
     * 
     * @param events
     * @param signalInitialized
     */
    protected abstract void process(SpeechRecognitionEvents events, CountDownLatch signalInitialized);

    public Optional<Throwable> getException() {
        try {
            return Optional.ofNullable(eventLoopException);
        } finally {
            eventLoopException = null;
        }
    }

    /**
     * Stop the event thread and clean up.
     */
    protected abstract void stopEventLoop();

    /**
     * Delete native objects
     */
    @Override
    protected abstract void dispose();

    @Override
    public void close() {
        eventLoopThread.interrupt();
        try {
            eventLoopThread.join();
            eventLoopThread = null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        super.close();
    }

}
