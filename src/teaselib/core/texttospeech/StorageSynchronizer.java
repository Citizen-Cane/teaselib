package teaselib.core.texttospeech;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import teaselib.Actor;
import teaselib.core.concurrency.NamedExecutorService;

/**
 * @author Citizen-Cane
 *
 */
class StorageSynchronizer {
    private final PrerecordedSpeechStorage storage;
    private final int nThreads;
    private final NamedExecutorService encoding;
    private final NamedExecutorService io = NamedExecutorService.singleThreadedQueue("Speech Recorder I/O");

    private final AtomicReference<Set<Future<?>>> tasks = new AtomicReference<>(new HashSet<>());

    StorageSynchronizer(PrerecordedSpeechStorage storage) {
        this.storage = storage;
        nThreads = 1;// Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        this.encoding = NamedExecutorService.newFixedThreadPool(getEncodingThreads(), "Speech Encoder",
                Integer.MAX_VALUE, TimeUnit.SECONDS);
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
        submitIO(() -> {
            storage.keepMessage(actor, voice, hash);
            return null;
        });
    }

    void deleteMessage(Actor actor, Voice voice, String hash) {
        submitIO(() -> {
            storage.deleteMessage(actor, voice, hash);
            return null;
        });
    }

    void createActorEntry(Actor actor, Voice voice, PreRecordedVoice prerecordedVoice) {
        submitIO(() -> {
            storage.createActorEntry(actor, voice, prerecordedVoice);
            return null;
        });
    }

    void createNewEntry(Actor actor, Voice voice, String hash, String messageHash) {
        submitIO(() -> {
            storage.createNewEntry(actor, voice, hash, messageHash);
            return null;
        });
    }

    Future<String> encode(Callable<String> task) {
        Future<String> encoderTask = encoding.submit(task);
        tasks.get().add(encoderTask);
        return encoderTask;
    }

    Future<String> storeRecordedSoundFile(Actor actor, Voice voice, String hash, String storedSoundFileNane,
            String recordedSoundFile) {
        return submitIO(() -> {
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
        submitIO(() -> {
            storage.writeStringResource(actor, voice, hash, name, value);
            return null;
        });
    }

    void close() throws InterruptedException, ExecutionException, IOException {
        submitIO(() -> {
            encoding.shutdown();
            io.shutdown();
        }).get();

        encoding.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        io.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        storage.close();
    }

    private Future<?> submitIO(Runnable task) {
        Future<?> ioTask = io.submit(task);
        tasks.get().add(ioTask);
        return ioTask;
    }

    private <T> Future<T> submitIO(Callable<T> task) {
        Future<T> ioTask = io.submit(task);
        tasks.get().add(ioTask);
        return ioTask;
    }

    public void checkForAsynchronousErrors() throws ExecutionException, InterruptedException {
        for (Future<?> future : tasks.getAndSet(new HashSet<>())) {
            if (future.isDone()) {
                future.get();
            } else {
                tasks.get().add(future);
            }
        }
    }

    public File assetPath() {
        return storage.assetPath();
    }

    public int getEncodingThreads() {
        return nThreads;
    }

    public int getUsedEncodingThreads() {
        return encoding.getLargestPoolSize();
    }
}
