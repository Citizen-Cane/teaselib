package teaselib.core.configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.util.ExceptionUtil;

public class PersistentConfigurationFiles {
    private final Set<PersistentConfigurationFile> elements = new HashSet<>();
    final NamedExecutorService fileWriterService = NamedExecutorService.newFixedThreadPool(1,
            "Configuration file writer");

    PersistentConfigurationFiles() {
        Thread fileWriterShutdownHook = new Thread(flushConfigfileWriteCache());
        Runtime.getRuntime().addShutdownHook(fileWriterShutdownHook);
    }

    PersistentConfigurationFile newFile(Path path) throws IOException {
        PersistentConfigurationFile persistentConfigurationFile = new PersistentConfigurationFile(path, this);
        elements.add(persistentConfigurationFile);
        return persistentConfigurationFile;
    }

    private Runnable flushConfigfileWriteCache() {
        return () -> {
            if (!fileWriterService.isShutdown()) {
                flush();
                fileWriterService.shutdown();
                try {
                    while (!fileWriterService.awaitTermination(1, TimeUnit.SECONDS)) {
                    }
                } catch (InterruptedException e) {
                    fileWriterService.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        };
    }

    public void flush() {
        elements.stream().filter(PersistentConfigurationFile::pendingChanges)
                .map(PersistentConfigurationFile::submitWriteTask).forEach(t -> {
                    try {
                        t.get();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new ScriptInterruptedException(e);
                    } catch (ExecutionException e) {
                        throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
                    }
                });
    }

}
