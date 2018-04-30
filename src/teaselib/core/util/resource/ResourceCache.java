package teaselib.core.util.resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ResourceCache {
    static final class Entry {
        String key;
        ResourceLocation resourceLocation;

        Entry(String data, ResourceLocation resourceLocation) {
            this.key = data;
            this.resourceLocation = resourceLocation;
        }

        @Override
        public String toString() {
            return key + "->" + resourceLocation;
        }
    }

    private Map<String, ResourceLocation> resourceLocations = new LinkedHashMap<>();
    private List<Entry> dataSequence = new ArrayList<>();
    private Map<String, ResourceLocation> dataLookup = new HashMap<>();

    public void add(ResourceLocation location) throws IOException {
        resourceLocations.put(location.root().toString(), location);
        List<String> resources = location.resources();
        for (String key : resources) {
            dataSequence.add(new Entry(key, location));
            dataLookup.put(key, location);
        }
    }

    public InputStream get(String key) throws IOException {
        ResourceLocation resourceLocation = getLocation(key);
        InputStream inputStream = resourceLocation.get(key);
        if (inputStream == null) {
            throw new IllegalArgumentException(key);
        }
        return inputStream;
    }

    private ResourceLocation getLocation(String key) throws IOException {
        ResourceLocation resourceLocation = dataLookup.get(key);
        if (resourceLocation == null) {
            throw new IOException(key);
        } else {
            return resourceLocation;
        }
    }

    public boolean has(String key) {
        return dataLookup.containsKey(key);
    }

    public List<String> get(Pattern pattern) {
        List<String> resources = new ArrayList<>();
        for (Entry entry : dataSequence) {
            if (pattern.matcher(entry.key).matches()) {
                resources.add(entry.key);
            }
        }
        return resources;
    }

    public static ResourceLocation location(String path, String project) throws IOException {
        if (path.toLowerCase().endsWith("jar") || path.toLowerCase().endsWith("zip")) {
            return new ZipLocation(Paths.get(path), Paths.get(project));
        } else {
            return new FolderLocation(Paths.get(path), Paths.get(project));
        }
    }
}
