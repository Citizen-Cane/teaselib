package teaselib.core.texttospeech;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
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
    private final int nThreads;
    private final NamedExecutorService encoding;
    private final NamedExecutorService io = NamedExecutorService.singleThreadedQueue("Speech Recorder I/O");
    private final Set<Future<?>> tasks = new CopyOnWriteArraySet<>();

    StorageSynchronizer(PrerecordedSpeechStorage storage) {
        this.storage = storage;
        this.nThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        this.encoding = NamedExecutorService.newFixedThreadPool(getEncodingThreads(), "Speech Encoder",
                Integer.MAX_VALUE, TimeUnit.SECONDS);
    }

    String getMessageHash(Actor actor, Voice voice, String hash) throws InterruptedException, ExecutionException {
        return submitIO(() -> storage.getMessageHash(actor, voice, hash)).get();
    }

    long lastModified(Actor actor, Voice voice, String hash) throws InterruptedException, ExecutionException {
        return submitIO(() -> storage.lastModified(actor, voice, hash)).get();
    }

    boolean hasMessage(Actor actor, Voice voice, String hash) throws InterruptedException, ExecutionException {
        return submitIO(() -> storage.hasMessage(actor, voice, hash)).get();
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

    Future<?> encode(Runnable task) {
        Future<?> encoderTask = encoding.submit(task);
        addAynchronousTask(encoderTask);
        return encoderTask;
    }

    Future<String> encode(Callable<String> task) {
        Future<String> encoderTask = encoding.submit(task);
        addAynchronousTask(encoderTask);
        return encoderTask;
    }

    Future<String> storeRecordedSoundFile(Actor actor, Voice voice, String hash, String recordedSoundFile,
            String storedSoundFileNane) {
        return submitIO(() -> {
            try (FileInputStream inputStream = new FileInputStream(recordedSoundFile);) {
                storage.storeSpeechResource(actor, voice, hash, inputStream, storedSoundFileNane);
            } finally {
                Files.delete(Paths.get(recordedSoundFile));
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
        addAynchronousTask(ioTask);
        return ioTask;
    }

    private <T> Future<T> submitIO(Callable<T> task) {
        Future<T> ioTask = io.submit(task);
        addAynchronousTask(ioTask);
        return ioTask;
    }

    private void addAynchronousTask(Future<?> ioTask) {
        tasks.add(ioTask);
    }

    public void checkForAsynchronousErrors() throws ExecutionException, InterruptedException {
        synchronized (tasks) {
            for (Future<?> task : tasks) {
                if (task.isDone()) {
                    task.get();
                    tasks.remove(task);
                }
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
