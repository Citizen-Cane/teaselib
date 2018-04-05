package teaselib.core.util.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ResourceCache {
    static final class Entry {
        public Entry(String data, ResourceLocation resourceLocation) {
            this.key = data;
            this.resourceLocation = resourceLocation;
        }

        String key;
        ResourceLocation resourceLocation;
    }

    private Map<String, ResourceLocation> resourceLocations = new LinkedHashMap<>();
    private List<Entry> dataSequence = new ArrayList<>();
    private Map<String, ResourceLocation> dataLookup = new HashMap<>();

    public void add(ResourceLocation location) throws IOException {
        resourceLocations.put(location.path().toString(), location);
        List<String> resources = location.resources();
        for (String key : resources) {
            dataSequence.add(new Entry(key, location));
            dataLookup.put(key, location);
        }
    }

    public InputStream get(String key) throws IOException {
        ResourceLocation resourceLocation = dataLookup.get(key);
        if (resourceLocation == null) {
            throw new IllegalArgumentException(key);
        }
        InputStream inputStream = resourceLocation.get(key);
        if (inputStream == null) {
            throw new IllegalArgumentException(key);
        }
        return inputStream;
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
}
