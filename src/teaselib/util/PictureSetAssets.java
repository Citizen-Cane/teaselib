package teaselib.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import teaselib.Resources;
import teaselib.core.AbstractImages;
import teaselib.core.PoseCache;
import teaselib.core.ResourceLoader;

class PictureSetAssets {

    private final Map<String, PictureSet> sets;
    private final Map<String, Take> resources;

    PictureSetAssets(Resources resources) {
        resources.removeIf(PoseCache::isPropertyFile);
        Map<String, Take> takes = gatherTakes(resources);
        Map<String, Pose> poses = gatherPoses(resources, takes);
        Map<String, Scene> scenes = gatherScenes(resources, takes, poses);
        this.sets = gatherSets(resources, takes, poses, scenes);
        this.resources = sets.entrySet().stream().map(Map.Entry::getValue).flatMap(PictureSet::stream)
                .flatMap(Scene::stream).flatMap(Pose::stream).flatMap(Take::entries)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Map<String, Take> gatherTakes(Resources resources) {
        Map<String, Take> takes = new LinkedHashMap<>();

        for (String resource : resources) {
            String parent = parent(resource);
            if (PathType.Take.matches(parent)) {
                takes.computeIfAbsent(parent, key -> new Take(key, resources)).add(resource);
            }
        }
        return takes;
    }

    private static Map<String, Pose> gatherPoses(Resources resources, Map<String, Take> takes) {
        Map<String, Pose> poses = new LinkedHashMap<>();
        for (Take take : takes.values()) {
            poses.computeIfAbsent(parent(take.key), Pose::new).add(take);
        }

        // Pose folders may contain images
        for (String resource : resources) {
            String parent = parent(resource);
            if (PathType.Pose.matches(parent)) {
                takes.computeIfAbsent(parent, key -> {
                    Take take = newParentLevelTake(resources, parent);
                    Pose pose = poses.computeIfAbsent(parent, Pose::new);

                    pose.add(take);
                    return take;
                });
            }
        }
        return poses;
    }

    private static Map<String, Scene> gatherScenes(Resources resources, Map<String, Take> takes,
            Map<String, Pose> poses) {
        Map<String, Scene> scenes = new LinkedHashMap<>();
        for (Pose pose : poses.values()) {
            scenes.computeIfAbsent(parent(pose.key), Scene::new).add(pose);
        }

        // Scene folders may contain images
        for (String resource : resources) {
            String parent = parent(resource);
            if (PathType.Scene.matches(parent)) {
                takes.computeIfAbsent(parent, key -> {
                    Take take = newParentLevelTake(resources, key);
                    Pose pose = poses.computeIfAbsent(key, Pose::new);
                    pose.add(take);

                    Scene scene = scenes.computeIfAbsent(key, Scene::new);
                    scene.add(pose);

                    return take;
                });
            }
        }
        return scenes;
    }

    private static Map<String, PictureSet> gatherSets(Resources resources, Map<String, Take> takes,
            Map<String, Pose> poses, Map<String, Scene> scenes) {
        Map<String, PictureSet> sets = new LinkedHashMap<>();
        for (Scene scene : scenes.values()) {
            sets.computeIfAbsent(parent(scene.key), PictureSet::new).add(scene);
        }

        // root & picture set folders may contain images
        addTake(resources, PathType.Set, sets, scenes, poses, takes);
        addTake(resources, PathType.Unknown, sets, scenes, poses, takes);
        return sets;
    }

    private static void addTake(Resources resources, PathType type, Map<String, PictureSet> sets,
            Map<String, Scene> scenes, Map<String, Pose> poses, Map<String, Take> takes) {
        for (String resource : resources) {
            String parent = parent(resource);
            if (type.matches(parent)) {
                takes.computeIfAbsent(parent, key -> {
                    Take take = newParentLevelTake(resources, key);
                    Pose pose = poses.computeIfAbsent(key, Pose::new);
                    pose.add(take);

                    Scene scene = scenes.computeIfAbsent(key, Scene::new);
                    scene.add(pose);
                    PictureSet set = sets.computeIfAbsent(key, PictureSet::new);
                    set.add(scene);

                    return take;
                });
            }
        }
    }

    private static Take newParentLevelTake(Resources resources, String key) {
        var take = new Take(key, resources);
        for (String r : resources) {
            if (parent(r).equals(key)) {
                take.add(r);
            }
        }
        return take;
    }

    enum PathType {
        Unknown,
        Ignore,
        Set,
        Scene,
        Pose,
        Take

        ;

        boolean matches(String resource) {
            if (this == Unknown) {
                if (Ignore.matches(resource)) {
                    return false;
                } else {
                    return !(Set.matches(resource) || Scene.matches(resource) || Pose.matches(resource)
                            || Take.matches(resource));
                }
            } else {
                String element = resource.substring(resource.lastIndexOf(ResourceLoader.separator) + 1);
                return element.startsWith(name());
            }
        }
    }

    private static String parent(String resource) {
        int index = resource.lastIndexOf('/');
        if (index == -1) {
            throw new NoSuchElementException("nth parent of " + resource);
        } else {
            return resource.substring(0, index);
        }
    }

    Stream<Map.Entry<String, Take>> resources() {
        return resources.entrySet().stream();
    }

    boolean contains(String resource) {
        return resources.containsKey(resource);
    }

    public void fetch(String resource) {
        resources.get(resource).images.fetch(resource);
    }

    public AnnotatedImage annotated(String resource) throws IOException, InterruptedException {
        return resources.get(resource).images.annotated(resource);
    }

    public boolean isEmpty() {
        return sets.isEmpty();
    }

    public int size() {
        return resources.size();
    }

    Collection<PictureSet> values() {
        return sets.values();
    }

    @Override
    public String toString() {
        return sets.toString();
    }

    abstract static class Assets<T> {

        final String key;
        final List<T> assets;

        Assets(String key) {
            this.key = key;
            this.assets = new ArrayList<>();
        }

        Assets(String key, List<T> assets) {
            this.key = key;
            this.assets = assets;
        }

        void add(T asset) {
            assets.add(asset);
        }

        Stream<T> stream() {
            return assets.stream();
        }

        @Override
        public String toString() {
            return assets.stream().map(s -> key(s).substring(key.length() + 1)).toList().toString();
        }

        abstract String key(T element);

    }

    static class PictureSet extends Assets<Scene> {
        PictureSet(String key) {
            super(key);
        }

        @Override
        String key(Scene element) {
            return element.key;
        }

    }

    static class Scene extends Assets<Pose> {
        Scene(String key) {
            super(key);
        }

        @Override
        String key(Pose element) {
            return element.key;
        }
    }

    static class Pose extends Assets<Take> {
        Pose(String key) {
            super(key);
        }

        @Override
        String key(Take element) {
            return element.key;
        }
    }

    static class Take extends Assets<String> {

        private class Entry implements Map.Entry<String, Take> {

            private final String element;

            private Entry(String key) {
                this.element = key;
            }

            @Override
            public String getKey() {
                return element;
            }

            @Override
            public Take getValue() {
                return Take.this;
            }

            @Override
            public Take setValue(Take value) {
                throw new UnsupportedOperationException();
            }
        }

        private final Map<String, String> mapping;
        final AbstractImages images;

        public Take(String key, Resources resources) {
            super(key, new ArrayList<>());
            this.mapping = new HashMap<>();
            this.images = new MoodImages(new Resources(resources, this.assets, this.mapping));
        }

        Stream<Map.Entry<String, Take>> entries() {
            return stream().map(Entry::new);
        }

        @Override
        String key(String element) {
            return element;
        }

        @Override
        void add(String asset) {
            super.add(asset);
            mapping.put(asset, asset);
        }

    }
}
