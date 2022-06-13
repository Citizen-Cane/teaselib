package teaselib;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

import org.slf4j.Logger;

import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;

public class Resources implements Iterable<String> {

    public final TeaseLib teaseLib;
    public final ResourceLoader loader;
    public final ExecutorService prefetch;
    private String pattern;
    public final List<String> elements;
    public final Map<String, String> mapping;

    public Resources(Resources resources, List<String> elements, Map<String, String> mapping) {
        this(resources.teaseLib, resources.loader, resources.prefetch, resources.pattern, elements, mapping);
    }

    public Resources(TeaseLib teaseLib, ResourceLoader loader, ExecutorService prefetch, String pattern,
            List<String> elements, Map<String, String> mapping) {
        this.teaseLib = teaseLib;
        this.loader = loader;
        this.prefetch = prefetch;
        this.pattern = pattern;
        this.elements = elements;
        this.mapping = mapping;
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public int size() {
        return elements.size();
    }

    public String get(int index) {
        return elements.get(index);
    }

    public boolean contains(String resource) {
        return mapping.containsKey(resource);
    }

    public void handleAssetNotFound(String resource, Logger logger) {
        if (teaseLib.config.getBoolean(Config.Debug.StopOnAssetNotFound)) {
            throw new NoSuchElementException(resource);
        } else {
            if (ResourceLoader.isAbsolute(resource)) {
                logger.warn("Resource \"" + resource + "\" not found in \"" + pattern + "\"");
            } else {
                String basePath = ResourceLoader.Paths.basePath(pattern);
                logger.warn("Resource \"" + basePath + resource + "\" not found in \"" + pattern + "\"");
            }
        }
    }

    public void removeIf(Predicate<String> predicate) {
        elements.removeIf(predicate);
        Set<String> related = new HashSet<>();
        mapping.entrySet().removeIf(entry -> {
            if (predicate.test(entry.getKey())) {
                related.add(entry.getValue());
                return true;
            } else if (predicate.test(entry.getValue())) {
                return true;
            } else {
                return false;
            }
        });
        related.forEach(mapping::remove);
    }

    @Override
    public Iterator<String> iterator() {
        return elements.iterator();
    }

    public byte[] getBytes(String resource) throws IOException {
        return loader.get(resource).readAllBytes();
    }

    @Override
    public String toString() {
        return pattern;
    }

}
