package teaselib.core.configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import teaselib.core.Closeable;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.concurrency.NoFuture;
import teaselib.core.util.ExceptionUtil;

public class PersistentConfigurationFileStoreService implements Closeable {

    private final Set<PersistentConfigurationFile> elements = new CopyOnWriteArraySet<>();
    private final NamedExecutorService fileWriterService = NamedExecutorService.newFixedThreadPool(1,
            "Configuration file writer service");

    private static final Future<Void> Done = NoFuture.Void;
    private Future<Void> writeAll = Done;

    ConfigurationFile open(Path path) throws IOException {
        return new PersistentConfigurationFile(path, this::queue);
    }

    private void queue(PersistentConfigurationFile file) {
        synchronized (elements) {
            elements.add(file);
        }
    }

    Future<Void> write() throws IOException {
        Set<PersistentConfigurationFile> pending;
        synchronized (elements) {
            if (!elements.isEmpty()) {
                if (!writeAll.isDone() && !writeAll.isCancelled()) {
                    try {
                        writeAll.get();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof IOException) {
                            throw (IOException) cause;
                        } else {
                            throw ExceptionUtil.asRuntimeException(e);
                        }
                    }
                }
                pending = new HashSet<>(elements);
                elements.clear();
            } else {
                pending = Collections.emptySet();
            }
        }

        if (!pending.isEmpty()) {
            writeAll = fileWriterService.submit(() -> {
                for (PersistentConfigurationFile file : pending) {
                    file.store();
                }
                return null;
            });
            return writeAll;
        } else {
            return Done;
        }
    }

    @Override
    public void close() {
        shutdown();
    }

    private void shutdown() {
        if (!fileWriterService.isShutdown()) {
            try {
                write();
            } catch (IOException e) {
                throw ExceptionUtil.asRuntimeException(e);
            } finally {
                fileWriterService.shutdown();
            }

            try {
                fileWriterService.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
