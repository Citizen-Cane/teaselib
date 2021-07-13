package teaselib.core.configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PersistentConfigurationFile extends ConfigurationFile {
    private final Path path;
    private final PersistentConfigurationFileStoreService fileWriterService;

    PersistentConfigurationFile(Path path,
            PersistentConfigurationFileStoreService persistentConfigurationFileStoreService) throws IOException {
        this.path = path;
        this.fileWriterService = persistentConfigurationFileStoreService;

        if (path.toFile().exists()) {
            try (var inputStream = Files.newInputStream(path)) {
                load(inputStream);
            }
        }
    }

    void store() throws IOException {
        var backupPath = path.resolveSibling(path.getFileName() + ".backup");
        var tempPath = path.resolveSibling(path.getFileName() + ".temp");
        try (var outputStream = Files.newOutputStream(tempPath)) {
            this.store(outputStream, "Teaselib settings file");
        }

        if (backupPath.toFile().exists()) {
            Files.delete(backupPath);
        }
        if (path.toFile().exists()) {
            Files.move(path, backupPath);
        }
        Files.move(tempPath, path);
    }

    @Override
    public void set(String key, String value) {
        synchronized (fileWriterService) {
            super.set(key, value);
            writeBackLater();
        }
    }

    @Override
    public void set(String key, boolean value) {
        synchronized (fileWriterService) {
            super.set(key, value);
            writeBackLater();
        }
    }

    @Override
    public void clear(String key) {
        synchronized (fileWriterService) {
            super.clear(key);
            writeBackLater();
        }
    }

    private void writeBackLater() {
        fileWriterService.queue(this);
    }

}
