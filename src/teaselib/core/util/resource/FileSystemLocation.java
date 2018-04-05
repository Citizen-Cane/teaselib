package teaselib.core.util.resource;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public abstract class FileSystemLocation implements ResourceLocation {
    protected List<Path> rootDirectories = new ArrayList<>();

    public FileSystemLocation() {
        super();

        rootDirectories = new ArrayList<>();
    }

    protected void add(Path path) {
        rootDirectories.add(path);
    }

    @Override
    public List<String> resources() throws IOException {
        List<String> resources = new ArrayList<>();
        for (Path root : rootDirectories) {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relative = file.subpath(root.getNameCount(), file.getNameCount());
                    resources.add("/" + relative.toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return resources;
    }
}
