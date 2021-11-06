package teaselib.core;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Citizen-Cane
 * 
 *         Synchronization class for synchronization between text-to-speech and speech recognition.
 *
 */
public final class AudioSync {
    private static final Logger logger = LoggerFactory.getLogger(AudioSync.class);

    private ReentrantLock sync = new ReentrantLock();

    public void startSpeechRecognition() {
        sync.lock();
    }

    public boolean speechRecognitionInProgress() {
        return sync.isLocked();
    }

    public void endSpeechRecognition() {
        sync.unlock();
    }

    public void completeSpeechRecognition() {
        runSynchronized(() -> { //
        }, "Waiting for speech recognition to complete", "Speech recognition in progress completed");
    }

    public void runSynchronizedSpeech(Runnable runnable) {
        String logStart = "Waiting for speech recognition to finish";
        String logSuccess = "Speech recognition finished";
        runSynchronized(runnable, logStart, logSuccess);
    }

    public void runSynchronizedSpeechRecognition(Runnable runnable) {
        String logStart = "Waiting for speech to complete";
        String logSuccess = "Speech completed";
        runSynchronized(runnable, logStart, logSuccess);
    }

    private void runSynchronized(Runnable runnable, String logStart, String logSuccess) {
        if (sync.isLocked()) {
            logger.info(logStart);
            try {
                sync.lockInterruptibly();
                try {
                    runnable.run();
                } finally {
                    sync.unlock();
                }
                logger.info(logSuccess);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            runnable.run();
        }
    }

}
