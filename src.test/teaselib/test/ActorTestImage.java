package teaselib.test;

import teaselib.Images;
import teaselib.util.AnnotatedImage;

public final class ActorTestImage implements Images {
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
        // Ignore
    }

    @Override
    public AnnotatedImage annotated(String resource) {
        return new AnnotatedImage(resource, null);
    }
}
