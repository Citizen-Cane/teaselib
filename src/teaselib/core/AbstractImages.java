package teaselib.core;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import java.util.Objects;

import teaselib.Images;
import teaselib.Resources;
import teaselib.core.Host.Location;
import teaselib.core.util.Prefetcher;
import teaselib.util.AnnotatedImage;

public abstract class AbstractImages implements Images {

    protected final Resources resources;
    public final PoseCache poseCache;
    private final Prefetcher<AnnotatedImage> imageFetcher;

    protected AbstractImages(Resources resources) {
        Objects.requireNonNull(resources);

        this.resources = resources;
        this.poseCache = new PoseCache(
                Paths.get(resources.script.teaseLib.host.getLocation(Location.User).getAbsolutePath(), "Pose cache"),
                resources.script);
        this.imageFetcher = new Prefetcher<>(resources.script.scriptRenderer.getPrefetchExecutorService(),
                this::annotatedImage);
    }

    private AnnotatedImage annotatedImage(String resource) throws IOException {
        byte[] image = resources.getBytes(resource);
        return poseCache.annotatedImage(resource, image);
    }

    @Override
    public boolean hasNext() {
        return !resources.isEmpty();
    }

    @Override
    public boolean contains(String resource) {
        return resources.contains(resource);
    }

    @Override
    public void fetch(String resource) {
        if (contains(resource)) {
            imageFetcher.fetch(resource);
        } else {
            throw new NoSuchElementException(resource);
        }
    }

    @Override
    public AnnotatedImage annotated(String resource) throws IOException, InterruptedException {
        if (contains(resource)) {
            return imageFetcher.get(resource);
        } else {
            throw new NoSuchElementException(resource);
        }
    }

}
