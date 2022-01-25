package teaselib.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Resources;
import teaselib.core.AbstractImages;
import teaselib.core.ResourceLoader;
import teaselib.core.Script;
import teaselib.core.events.EventArgs;
import teaselib.core.util.ExceptionUtil;

public class SceneBasedImages extends AbstractImages {

    static final Logger logger = LoggerFactory.getLogger(Script.class);

    private final Map<String, PictureSet> pictureSets;

    // TODO chapters
    // TODO sections are defined as afterChoices, but should be before saying s.o.

    private static final int numberOfChoicesToKeepTake = 0;
    private static final int numberOfChoicesToKeepPose = 0;
    private static final int numberOfPosesToKeepScene = 0;
    private static final int numberOfScenesToKeepSet = 0;

    private int remainingSetScenes = numberOfScenesToKeepSet;
    private int remainingScenePoses = numberOfPosesToKeepScene;
    private int remainingPoseTakes = numberOfChoicesToKeepPose;
    private int remainingTakeViews = numberOfChoicesToKeepTake;

    private PictureSet currentSet;
    private Scene currentScene;
    private Pose currentPose;
    private Take currentTake;

    public SceneBasedImages(Resources resources) {
        super(resources);

        if (resources.isEmpty()) {
            String any = "";
            currentTake = new Take(any, resources.script);

            currentPose = new Pose(any);
            currentPose.add(currentTake);
            currentScene = new Scene(any);
            currentScene.add(currentPose);
            currentSet = new PictureSet(any);
            currentSet.add(currentScene);
            pictureSets = new HashMap<>();
            pictureSets.put(any, currentSet);
        } else {
            logger.info("Fetching {} images", resources.size());
            pictureSets = pictureSets(resources);
            chooseSet();
            try {
                resources.script.scriptRenderer.getPrefetchExecutorService().submit(() -> {
                    logger.info("Anylyzed {} images", resources.size());
                }).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                throw ExceptionUtil.asRuntimeException(e);
            }
            resources.script.scriptRenderer.events.afterPrompt.add(this::handleAfterChoices);
            resources.script.scriptRenderer.events.beforeMessage.add(this::handleBeforeMessage);
        }
    }

    /**
     * @param e
     *            Not used
     */
    void handleAfterChoices(EventArgs e) {
        remainingTakeViews--;
        if (remainingTakeViews <= 0) {
            remainingPoseTakes--;
            if (remainingPoseTakes <= 0) {
                remainingScenePoses--;
                if (remainingScenePoses <= 0) {
                    remainingSetScenes--;
                }
            }
        }
    }

    // TODO too late to change the image set, because the images are needed when the message is built,
    // but the BeforeMessage event happens when the message is ready and about to be rendered

    /**
     * @param e
     *            Not used
     */
    void handleBeforeMessage(EventArgs e) {
        if (remainingSetScenes <= 0) {
            chooseSet();
        } else if (remainingScenePoses <= 0) {
            chooseScene();
        } else if (remainingPoseTakes <= 0) {
            choosePose();
        } else if (remainingTakeViews <= 0) {
            chooseTake();
        }
    }

    private void chooseSet() {
        // TODO random / linear
        // TODO persistence (choose set once per session)
        currentSet = resources.script.random.item(currentSet, new ArrayList<>(pictureSets.values()));
        // TODO play each scene -> shuffle list
        remainingSetScenes = currentSet.scenes.size();
        chooseScene();
    }

    private void chooseScene() {
        // TODO random / linear
        // TODO intro outro
        currentScene = resources.script.random.item(currentScene, currentSet.scenes);
        // TODO play each scene -> shuffle list
        remainingScenePoses = currentScene.poses.size();
        choosePose();
    }

    void choosePose() {
        // TODO random / linear
        currentPose = resources.script.random.item(currentPose, currentScene.poses);
        // TODO play each pose -> shuffle list
        remainingPoseTakes = currentPose.takes.size();
        chooseTake();
    }

    void chooseTake() {
        // TODO random / linear
        currentTake = resources.script.random.item(currentTake, currentPose.takes);
        // TODO play each pose -> shuffle list
        remainingTakeViews = currentTake.resources.size();
    }

    private Map<String, PictureSet> pictureSets(Resources resources) {
        Map<String, Take> takes = new LinkedHashMap<>();
        for (String resource : resources) {
            String parent = parent(resource);
            if (PathType.Take.matches(parent)) {
                takes.computeIfAbsent(parent, key -> new Take(key, resources.script)).add(resource);
            }
        }

        Map<String, Pose> poses = new LinkedHashMap<>();
        for (Take take : takes.values()) {
            poses.computeIfAbsent(parent(take.key), key -> new Pose(key)).add(take);
        }

        Map<String, Scene> scenes = new LinkedHashMap<>();
        for (Pose pose : poses.values()) {
            scenes.computeIfAbsent(parent(pose.key), Scene::new).add(pose);
        }

        // Pose folders may contain images
        for (String resource : resources) {
            String parent = parent(resource);
            if (PathType.Pose.matches(parent)) {
                takes.computeIfAbsent(parent, key -> {
                    Take take = newParentLevelTake(parent);
                    Pose pose = poses.computeIfAbsent(parent, Pose::new);

                    pose.add(take);
                    return take;
                });
            }
        }

        // Scene folders may contain images
        for (String resource : resources) {
            String parent = parent(resource);
            if (PathType.Scene.matches(parent)) {
                takes.computeIfAbsent(parent, key -> {
                    Take take = newParentLevelTake(key);
                    Pose pose = poses.computeIfAbsent(key, Pose::new);
                    pose.add(take);

                    Scene scene = scenes.computeIfAbsent(key, Scene::new);
                    scene.add(pose);

                    return take;
                });
            }
        }

        Map<String, PictureSet> sets = new LinkedHashMap<>();
        for (Scene scene : scenes.values()) {
            sets.computeIfAbsent(parent(scene.key), PictureSet::new).add(scene);
        }

        // root & picture set folders
        addTake(resources, PathType.Set, sets, scenes, poses, takes);
        addTake(resources, PathType.Unknown, sets, scenes, poses, takes);

        return sets;
    }

    private void addTake(Resources resources, PathType type, Map<String, PictureSet> sets, Map<String, Scene> scenes,
            Map<String, Pose> poses, Map<String, Take> takes) {
        for (String resource : resources) {
            String parent = parent(resource);
            if (type.matches(parent)) {
                takes.computeIfAbsent(parent, key -> {
                    Take take = newParentLevelTake(key);
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

    private Take newParentLevelTake(String key) {
        var take = new Take(key, resources.script);
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

    public static String parent(String resource) {
        int index = resource.lastIndexOf('/');
        if (index == -1) {
            throw new NoSuchElementException("nth parent of " + resource);
        } else {
            return resource.substring(0, index);
        }
    }

    @Override
    public boolean hasNext() {
        return currentTake.images.hasNext();
    }

    @Override
    public String next(String... hints) {
        return currentTake.images.next(hints);
    }

    @Override
    public boolean contains(String resource) {
        return resources.contains(resource);
    }

    @Override
    public AnnotatedImage annotated(String resource) throws IOException, InterruptedException {
        return currentTake.images.annotated(resource);
    }

    @Override
    public String toString() {
        return pictureSets.toString();
    }

    private static class PictureSet {
        final String key;
        final List<Scene> scenes;

        public PictureSet(String key) {
            this.key = key;
            this.scenes = new ArrayList<>();
        }

        public void add(Scene scene) {
            scenes.add(scene);
        }

        @Override
        public String toString() {
            return scenes.stream().map(s -> s.key.substring(key.length() + 1)).toList().toString();
        }
    }

    private static class Scene {
        final String key;
        final List<Pose> poses;

        public Scene(String key) {
            this.key = key;
            this.poses = new ArrayList<>();
        }

        public void add(Pose pose) {
            poses.add(pose);
        }

        @Override
        public String toString() {
            return poses.stream().map(s -> s.key.substring(key.length() + 1)).toList().toString();
        }
    }

    private static class Pose {
        final String key;
        final List<Take> takes;

        public Pose(String key) {
            this.key = key;
            this.takes = new ArrayList<>();
        }

        public void add(Take take) {
            takes.add(take);
        }

        @Override
        public String toString() {
            return takes.stream().map(s -> s.key.substring(key.length() + 1)).toList().toString();
        }
    }

    private static class Take {
        final String key;
        final Resources resources;
        final MoodImages images;

        Take(String key, Script script) {
            this.key = key;
            this.resources = new Resources(script, Collections.emptyList());
            this.images = new MoodImages(this.resources);
        }

        public void add(String resource) {
            resources.add(resource);
        }

        @Override
        public String toString() {
            return resources.stream().map(s -> s.substring(key.length() + 1)).toList().toString();
        }

    }

}
