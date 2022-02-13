package teaselib.test;

import java.util.NoSuchElementException;

import teaselib.ActorImages;
import teaselib.ImageCollection;
import teaselib.util.AnnotatedImage;

public final class ActorTestImage implements ActorImages, ImageCollection {
    private final String resourcePath;

    public ActorTestImage(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public String next(String... hints) {
        return resourcePath;
    }

    @Override
    public boolean contains(String resource) {
        return resourcePath.equals(resource);
    }

    @Override
    public void fetch(String resource) {
        if (!contains(resource))
            throw new NoSuchElementException(resource);
    }

    @Override
    public AnnotatedImage annotated(String resource) {
        return new AnnotatedImage(resource, null);
    }
}
