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

    public void start() {
        sync.lock();
    }

    public boolean inProgress() {
        return sync.isLocked();
    }

    public void stop() {
        sync.unlock();
    }

    public void completeSpeechRecognition() {
        synchronize(() -> { //
        }, "Waiting for speech recognition to complete");
    }

    public void synchronizeTextToSpeech(Runnable runnable) {
        String logStart = "Waiting for speech recognition to finish";
        synchronize(runnable, logStart);
    }

    public void synchronizeSpeechRecognition(Runnable runnable) {
        String logStart = "Waiting for speech to complete";
        synchronize(runnable, logStart);
    }

    private void synchronize(Runnable runnable, String logStart) {
        try {
            boolean tryLocked = sync.tryLock();
            if (!tryLocked) {
                logger.info(logStart);
                sync.lockInterruptibly();
            }
            try {
                runnable.run();
            } finally {
                sync.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
