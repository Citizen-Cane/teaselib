package teaselib.test;

import teaselib.Images;
import teaselib.util.AnnotatedImage;

public final class ActorTestImage implements Images {
    private final String resourcePath;

    public ActorTestImage(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @Override
    public String next() {
        return resourcePath;
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public void hint(String... hint) { // ignore
    }

    @Override
    public boolean contains(String resource) {
        return resourcePath.equals(resource);
    }

    @Override
    public AnnotatedImage annotated(String resource) {
        return new AnnotatedImage(resource, null);
    }
}