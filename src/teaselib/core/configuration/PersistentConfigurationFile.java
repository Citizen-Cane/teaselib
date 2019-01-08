package teaselib.core.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Timer;

public class PersistentConfigurationFile extends ConfigurationFile {
    private static final long serialVersionUID = 1L;

    private final Path path;
    private final ExecutorService fileWriterService;

    private final AtomicBoolean writeBack = new AtomicBoolean(false);
    private final Timer writeBackDelay;

    PersistentConfigurationFile(Path path, PersistentConfigurationFiles persistentConfigurationFiles)
            throws IOException {
        this.path = path;
        this.fileWriterService = persistentConfigurationFiles.fileWriterService;

        if (path.toFile().exists()) {
            try (InputStream inputStream = Files.newInputStream(path)) {
                load(inputStream);
            }
        }

        writeBackDelay = new Timer(1000, e -> {
            submitWriteTask();
        });
        writeBackDelay.setRepeats(false);
    }

    Future<Void> submitWriteTask() {
        synchronized (PersistentConfigurationFile.this) {
            writeBackDelay.stop();
            boolean mustWrite = writeBack.getAndSet(false);
            if (mustWrite) {
                // TODO Remember and forward exception
                return fileWriterService.submit(this::writeTask);
            } else {
                return null;
            }
        }
    }

    private Void writeTask() throws IOException {
        Path backupPath = path.resolveSibling(path.getFileName() + ".backup");
        Path tempPath = path.resolveSibling(path.getFileName() + ".temp");
        try (OutputStream outputStream = Files.newOutputStream(tempPath)) {
            // TODO Properties base class is doomed anyway, just replace the cruft with something good
            this.save(outputStream, "Teaselib settings file");
        }

        if (backupPath.toFile().exists()) {
            Files.delete(backupPath);
        }
        if (path.toFile().exists()) {
            Files.move(path, backupPath);
        }
        Files.move(tempPath, path);

        return null;
    }

    @Override
    public void set(String key, String value) {
        synchronized (this) {
            super.set(key, value);
            writeBackLater();
        }
    }

    @Override
    public void set(String key, boolean value) {
        synchronized (this) {
            super.set(key, value);
            writeBackLater();
        }
    }

    @Override
    public void clear(String key) {
        synchronized (this) {
            super.clear(key);
            writeBackLater();
        }
    }

    private void writeBackLater() {
        if (!writeBack.getAndSet(true)) {
            writeBackDelay.start();
        }
    }

    public boolean pendingChanges() {
        return writeBack.get();
    }
}
