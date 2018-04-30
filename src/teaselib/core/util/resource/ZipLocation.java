package teaselib.core.util.resource;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ZipLocation extends FileSystemLocation {
    private final FileSystem fileSystem;

    public ZipLocation(Path zip) throws IOException {
        this(zip, Paths.get(""));
    }

    ZipLocation(Path zip, Path project) throws IOException {
        super(zip, project);
        this.fileSystem = FileSystems.newFileSystem(zip, null);
        for (Path root : fileSystem.getRootDirectories()) {
            addRootDirectory(root);
        }
    }

    @Override
    public void close() throws IOException {
        fileSystem.close();
    }
}
