package teaselib.util;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import teaselib.Resources;
import teaselib.core.AbstractImages;
import teaselib.core.events.EventArgs;
import teaselib.core.util.Prefetcher;

public class SceneBasedImages extends AbstractImages {

    private final Map<String, PictureSet> pictureSets;

    // TODO chapters
    // TODO sections are defined as afterChoices, but should be before saying s.o.

    private static final int numberOfChoicesToKeepPose = 0;
    private static final int numberOfPosesToKeepScene = 0;
    private static final int numberOfScenesToKeepSet = 0;

    private int remainingSetScenes = numberOfScenesToKeepSet;
    private int remainingScenePoses = numberOfPosesToKeepScene;
    private int remainingPoseViews = numberOfChoicesToKeepPose;

    private PictureSet currentSet;
    private Scene currentScene;
    private Pose currentPose;

    public SceneBasedImages(Resources resources) {
        super(resources);

        if (resources.isEmpty()) {
            String any = "";
            currentPose = new Pose(any, resources);
            currentScene = new Scene(any);
            currentScene.add(currentPose);
            currentSet = new PictureSet(any);
            currentSet.add(currentScene);
            pictureSets = new HashMap<>();
            pictureSets.put(any, currentSet);
        } else {
            pictureSets = pictureSets(resources);
            chooseSet();

            resources.script.scriptRenderer.events.afterChoices.add(this::handleAfterChoices);
            resources.script.scriptRenderer.events.beforeMessage.add(this::handleBeforeMessage);
        }
    }

    void handleAfterChoices(EventArgs e) {
        remainingPoseViews--;
        if (remainingPoseViews <= 0) {
            remainingScenePoses--;
            if (remainingScenePoses <= 0) {
                remainingSetScenes--;
            }
        }
    }

    // TODO too late to change the image set, because the images are needed when the message is built,
    // but the BeforeMessage event happens when the message is ready and about to be rendered

    void handleBeforeMessage(EventArgs e) {
        if (remainingSetScenes <= 0) {
            chooseSet();
        } else if (remainingScenePoses <= 0) {
            chooseScene();
        } else if (remainingPoseViews <= 0) {
            choosePose();
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
        remainingPoseViews = currentPose.resources.size();
    }

    private static Map<String, PictureSet> pictureSets(Resources resources) {
        Map<String, Pose> poses = new LinkedHashMap<>();

        for (String resource : resources) {
            poses.computeIfAbsent(parent(resource), key -> new Pose(key, resources)).add(resource);
        }

        Map<String, Scene> scenes = new LinkedHashMap<>();
        for (Pose pose : poses.values()) {
            scenes.computeIfAbsent(parent(pose.key), Scene::new).add(pose);
        }

        Map<String, PictureSet> sets = new LinkedHashMap<>();
        for (Scene scene : scenes.values()) {
            sets.computeIfAbsent(parent(scene.key), PictureSet::new).add(scene);
        }

        return sets;
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
        return currentPose.images.hasNext();
    }

    @Override
    public String next() {
        return currentPose.images.next();
    }

    @Override
    public boolean contains(String resource) {
        return resources.contains(resource);
    }

    @Override
    public void hint(String... hint) {
        // TODO toys
        // TODO body state (slave standing or kneeling)
        currentPose.images.hint(hint);
    }

    @Override
    public Prefetcher<AnnotatedImage> prefetcher() {
        return currentPose.images.prefetcher();
    }

    @Override
    public AnnotatedImage annotated(String resource) throws IOException, InterruptedException {
        return currentPose.images.annotated(resource);
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
            return scenes.stream().map(s -> s.key.substring(key.length() + 1)).collect(toList()).toString();
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
            return poses.stream().map(s -> s.key.substring(key.length() + 1)).collect(toList()).toString();
        }
    }

    private static class Pose {
        final String key;
        final Resources resources;
        final MoodImages images;

        public Pose(String key, Resources resources) {
            this.key = key;
            this.resources = new Resources(resources.script, Collections.emptyList());
            this.images = new MoodImages(resources);
        }

        public void add(String resource) {
            resources.add(resource);
        }

        @Override
        public String toString() {
            return resources.stream().map(s -> s.substring(key.length() + 1)).collect(toList()).toString();
        }

    }

}
