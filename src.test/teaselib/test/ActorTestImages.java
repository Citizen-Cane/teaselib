package teaselib.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import teaselib.ActorImages;
import teaselib.ImageCollection;
import teaselib.util.AnnotatedImage;

public final class ActorTestImages implements ActorImages, ImageCollection {
    private final List<String> resources;
    private final List<String> fetched;
    private Iterator<String> current;

    public ActorTestImages(String... resources) {
        this(Arrays.asList(resources), new ArrayList<>());
    }

    public ActorTestImages(List<String> resources, List<String> fetched) {
        this.resources = resources;
        this.fetched = fetched;
        this.current = this.resources.iterator();
    }

    @Override
    public boolean hasNext() {
        return current.hasNext();
    }

    @Override
    public String next(String... hints) {
        String resource = current.next();
        if (!current.hasNext()) {
            current = resources.iterator();
        }
        return resource;
    }

    @Override
    public boolean contains(String resource) {
        return resources.contains(resource);
    }

    @Override
    public void fetch(String resource) {
        if (contains(resource)) {
            fetched.add(resource);
        } else {
            throw new NoSuchElementException(resource);
        }
    }

    @Override
    public AnnotatedImage annotated(String resource) {
        if (contains(resource)) {
            return new AnnotatedImage(resource, null);
        } else {
            throw new NoSuchElementException(resource);
        }
    }

    public List<String> getFetched() {
        return fetched;
    }

}
