package teaselib.test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import teaselib.Images;
import teaselib.util.AnnotatedImage;

public final class ActorTestImages implements Images {
    private final List<String> resources;
    private Iterator<String> current;

    public ActorTestImages(String... resources) {
        this.resources = Arrays.asList(resources);
        current = this.resources.iterator();
    }

    @Override
    public String next() {
        String resource = current.next();
        if (!current.hasNext()) {
            current = resources.iterator();
        }
        return resource;
    }

    @Override
    public boolean hasNext() {
        return current.hasNext();
    }

    @Override
    public void hint(String... hint) { // ignore
    }

    @Override
    public boolean contains(String resource) {
        return resources.contains(resource);
    }

    @Override
    public AnnotatedImage annotated(String resource) {
        return new AnnotatedImage(resource, null);
    }
}