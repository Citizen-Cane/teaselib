package teaselib.core.util.resource;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FolderLocation extends FileSystemLocation {
    public FolderLocation(Path root) {
        this(root, Paths.get(""));
    }

    FolderLocation(Path root, Path project) {
        super(root, project);
        addRootDirectory(root);
    }

    @Override
    public void close() throws IOException {
        return;
    }
}
