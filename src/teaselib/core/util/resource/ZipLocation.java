package teaselib.core.util.resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class ZipLocation extends FileSystemLocation {
    private final Path path;
    private final FileSystem fileSystem;

    public ZipLocation(Path path) throws IOException {
        this.path = path;
        this.fileSystem = FileSystems.newFileSystem(path, null);
        for (Path root : fileSystem.getRootDirectories()) {
            add(root);
        }
    }

    @Override
    public Path path() {
        return path;
    }

    @Override
    public InputStream get(String resource) throws IOException {
        return Files.newInputStream(fileSystem.getPath(resource));
    }

    @Override
    public void close() throws IOException {
        fileSystem.close();
    }
}
