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

    public enum AudioType {
        Recognition,
        Speech
    }

    private ReentrantLock sync = new ReentrantLock();
    private AudioType audioType = null;

    public void start(AudioType audioType) {
        sync.lock();
        this.audioType = audioType;
    }

    private boolean inProgress(AudioType audioType) {
        return this.audioType == audioType;
    }

    public boolean isHeldByCurrentThread() {
        return sync.isHeldByCurrentThread();
    }

    public void stop() {
        audioType = null;
        sync.unlock();
    }

    public void completeSpeechRecognition() {
        if (inProgress(AudioType.Recognition)) {
            synchronize(null, () -> { //
            }, "Waiting for speech recognition to complete");
        }
    }

    public void synchronizeTextToSpeech(Runnable runnable) {
        String logStart = "Waiting for speech recognition to finish";
        synchronize(AudioType.Speech, runnable, logStart);
    }

    public void synchronizeSpeechRecognition(Runnable runnable) {
        String logStart = "Waiting for speech to complete";
        synchronize(AudioType.Recognition, runnable, logStart);
    }

    private void synchronize(AudioType audioType, Runnable runnable, String logStart) {
        try {
            boolean tryLocked = sync.tryLock();
            if (!tryLocked) {
                logger.info(logStart);
                sync.lockInterruptibly();
            }
            this.audioType = audioType;
            try {
                runnable.run();
            } finally {
                this.audioType = null;
                sync.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
