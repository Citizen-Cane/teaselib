package teaselib.core.util.resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FolderLocation extends FileSystemLocation {
    private final Path path;

    public FolderLocation(Path path) {
        super();
        this.path = path;
        add(path);
    }

    @Override
    public Path path() {
        return rootDirectories.get(0);
    }

    @Override
    public InputStream get(String resource) throws IOException {
        String relative = resource.substring(1);
        return Files.newInputStream(path.resolve(Paths.get(relative)));
    }

    @Override
    public void close() throws IOException {
        return;
    }
}
