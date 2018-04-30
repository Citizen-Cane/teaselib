package teaselib.core.util.resource;

import java.io.IOException;
import java.nio.file.Path;

public class FolderLocation extends FileSystemLocation {
    public FolderLocation(Path root) {
        this(root, null);
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
