package teaselib;

import java.io.IOException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Script;
import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.Prefetcher;
import teaselib.util.AnnotatedImage;

public abstract class CachedImages implements Images {

    private static final Logger logger = LoggerFactory.getLogger(Script.class);

    protected final Resources resources;
    private final Prefetcher<AnnotatedImage> imageFetcher;

    public CachedImages(Resources resources) {
        Objects.requireNonNull(resources);
        this.resources = resources;
        this.imageFetcher = new Prefetcher<>(resources.script.scriptRenderer.getPrefetchExecutorService(),
                this::annotatedImage);
    }

    protected abstract AnnotatedImage annotatedImage(String resource) throws IOException;

    @Override
    public boolean contains(String resource) {
        return resources.contains(resource);
    }

    @Override
    public void fetch(String resource) {
        if (contains(resource)) {
            imageFetcher.fetch(resource);
        } else {
            handleAssetNotFound(resource);
        }
    }

    protected void handleAssetNotFound(String resource) {
        ExceptionUtil.handleAssetNotFound(new IOException(resource), resources.script.teaseLib.config, logger);
    }

    @Override
    public AnnotatedImage annotated(String resource) throws IOException, InterruptedException {
        if (contains(resource)) {
            String path = resources.mapping.get(resource);
            return imageFetcher.get(path);
        } else {
            handleAssetNotFound(resource);
            return AnnotatedImage.NoImage;
        }
    }

    @Override
    public String toString() {
        return resources.toString();
    }

}
