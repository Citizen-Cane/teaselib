package teaselib.core.configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PersistentConfigurationFile extends ConfigurationFileImpl {
    private final Path path;

    interface ChangeListener {
        void fileChanged(PersistentConfigurationFile file);
    }

    private final ChangeListener changeListener;

    PersistentConfigurationFile(Path path, ChangeListener changeListener) throws IOException {
        this.path = path;
        this.changeListener = changeListener;

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
            synchronized (this) {
                store(outputStream, "Teaselib settings file");
            }
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
        changeListener.fileChanged(this);
    }

}
