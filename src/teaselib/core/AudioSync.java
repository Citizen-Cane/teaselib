package teaselib.core;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Citizen-Cane
 * 
 *         Negotiator class for synchronization for speech and speech recognition.
 *
 */
public final class AudioSync {
    private static final Logger logger = LoggerFactory.getLogger(AudioSync.class);

    private ReentrantLock sync = new ReentrantLock();

    public void completeSpeechRecognition() {
        if (sync.isLocked()) {
            logger.info("Waiting for speech recognition to complete");
            try {
                sync.lockInterruptibly();
                sync.unlock();
                logger.info("Speech recognition in progress completed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean speechRecognitionInProgress() {
        return sync.isLocked();
    }

    public void produceSpeech(Runnable runnable) {
        if (sync.isLocked()) {
            try {
                sync.lockInterruptibly();
                try {
                    runnable.run();
                } finally {
                    sync.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            runnable.run();
        }
    }

    public void startSpeechRecognition() {
        sync.lock();
    }

    public void endSpeechRecognition() {
        sync.unlock();
    }

    public void whenSpeechCompleted(Runnable runnable) {
        if (sync.isLocked()) {
            logger.info("Waiting for speech to complete");
            try {
                sync.lockInterruptibly();
                try {
                    runnable.run();
                } finally {
                    sync.unlock();
                    logger.info("Speech completed");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            runnable.run();
        }
    }
}