package teaselib.core.util.resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public abstract class FileSystemLocation implements ResourceLocation {
    final Path root;
    final Path project;

    List<Path> rootDirectories = new ArrayList<>();

    FileSystemLocation(Path root, Path project) {
        if (project.startsWith("/"))
            throw new IllegalArgumentException("Project path must be relative to resolve it");
        this.root = root;
        this.project = project;

        rootDirectories = new ArrayList<>();
    }

    void addRootDirectory(Path path) {
        rootDirectories.add(path.resolve(project.toString()));
        return;
    }

    @Override
    public Path root() {
        return root;
    }

    @Override
    public Path project() {
        return project;
    }

    @Override
    public InputStream get(String resource) throws IOException {
        String relative = resource.substring(1);
        return Files.newInputStream(rootDirectories.get(0).resolve(relative));
    }

    @Override
    public List<String> resources() throws IOException {
        List<String> resources = new ArrayList<>();
        for (Path directory : rootDirectories) {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relative = file.subpath(directory.getNameCount(), file.getNameCount());
                    String key = "/" + relative.toString().replace('\\', '/');
                    resources.add(key);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return resources;
    }

    @Override
    public String toString() {
        return root + (!project.toString().isEmpty() ? " -> " + project : "");
    }
}
