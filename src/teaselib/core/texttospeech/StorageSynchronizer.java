package teaselib.core.texttospeech;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import teaselib.Actor;
import teaselib.core.concurrency.NamedExecutorService;

/**
 * @author Citizen-Cane
 *
 */
class StorageSynchronizer {
    private final PrerecordedSpeechStorage storage;
    private final ExecutorService encoding = NamedExecutorService.newFixedThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() - 1), "Speech Encoder", 10, TimeUnit.SECONDS);
    private final ExecutorService io = NamedExecutorService.singleThreadedQueue("Speech Recorder I/O");

    StorageSynchronizer(PrerecordedSpeechStorage storage) {
        this.storage = storage;
    }

    String getMessageHash(Actor actor, Voice voice, String hash) throws InterruptedException, ExecutionException {
        return io.submit(() -> storage.getMessageHash(actor, voice, hash)).get();
    }

    long lastModified(Actor actor, Voice voice, String hash) throws InterruptedException, ExecutionException {
        return io.submit(() -> storage.lastModified(actor, voice, hash)).get();
    }

    boolean hasMessage(Actor actor, Voice voice, String hash) throws InterruptedException, ExecutionException {
        return io.submit(() -> storage.hasMessage(actor, voice, hash)).get();
    }

    void keepMessage(Actor actor, final Voice voice, String hash) {
        io.submit(() -> {
            storage.keepMessage(actor, voice, hash);
            return null;
        });
    }

    void deleteMessage(Actor actor, Voice voice, String hash) {
        io.submit(() -> {
            storage.deleteMessage(actor, voice, hash);
            return null;
        });
    }

    void createActorEntry(Actor actor, Voice voice, PreRecordedVoice prerecordedVoice) {
        io.submit(() -> {
            storage.createActorEntry(actor, voice, prerecordedVoice);
            return null;
        });
    }

    void createNewEntry(Actor actor, Voice voice, String hash, String messageHash) {
        io.submit(() -> {
            storage.createNewEntry(actor, voice, hash, messageHash);
            return null;
        });
    }

    Future<String> encode(Callable<String> encode) {
        return encoding.submit(encode);
    }

    Future<String> storeRecordedSoundFile(Actor actor, Voice voice, String hash, String storedSoundFileNane,
            String recordedSoundFile) {
        return io.submit(() -> {
            try (FileInputStream inputStream = new FileInputStream(recordedSoundFile);) {
                storage.storeSpeechResource(actor, voice, hash, inputStream, storedSoundFileNane);
            }
            if (!new File(recordedSoundFile).delete()) {
                throw new IllegalStateException("Can't delete temporary encoded speech file " + recordedSoundFile);
            }
            return storedSoundFileNane;
        });
    }

    void writeStringResource(Actor actor, Voice voice, String hash, String name, String value) {
        io.submit(() -> {
            storage.writeStringResource(actor, voice, hash, name, value);
            return null;
        });
    }

    void close() throws InterruptedException, ExecutionException, IOException {
        io.submit(() -> {
            encoding.shutdown();
            io.shutdown();
        }).get();

        encoding.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        io.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        storage.close();
    }

    /**
     * @return
     */
    public File assetPath() {
        return storage.assetPath();
    }
}
