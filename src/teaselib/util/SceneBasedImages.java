package teaselib.util;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import teaselib.Images;
import teaselib.TeaseScript;
import teaselib.core.events.EventArgs;

public class SceneBasedImages implements teaselib.Images {

    private final TeaseScript script;
    private final List<String> resources;
    private final Map<String, PictureSet> pictureSets;

    // TODO chapters
    // TODO sections are defined as afterChoices, but should be before saying s.o.
    private final static int numberOfChoicesToKeepPose = 0;
    private final static int numberOfPosesToKeepScene = 0;
    private final static int numberOfScenesToKeepSet = 0;

    private int remainingSetScenes = numberOfScenesToKeepSet;
    private int remainingScenePoses = numberOfPosesToKeepScene;
    private int remainingPoseViews = numberOfChoicesToKeepPose;

    private PictureSet currentSet;
    private Scene currentScene;
    private Pose currentPose;

    private Iterator<String> images = Images.None;

    public SceneBasedImages(TeaseScript script, List<String> resources) {
        Objects.requireNonNull(script);

        this.script = script;
        this.resources = resources;
        if (resources.isEmpty()) {
            this.pictureSets = Collections.emptyMap();
        } else {
            this.pictureSets = pictureSets(resources);
            chooseSet();

            script.scriptRenderer.events.afterChoices.add(this::handleAfterChoices);
            script.scriptRenderer.events.beforeMessage.add(this::handleBeforeMessage);
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
        currentSet = script.random(currentSet, new ArrayList<>(pictureSets.values()));
        // TODO play each scene -> shuffle list
        remainingSetScenes = currentSet.scenes.size();
        chooseScene();
    }

    private void chooseScene() {
        // TODO random / linear
        // TODO intro outro
        currentScene = script.random(currentScene, currentSet.scenes);
        // TODO play each scene -> shuffle list
        remainingScenePoses = currentScene.poses.size();
        choosePose();
    }

    void choosePose() {
        // TODO random / linear
        currentPose = script.random(currentPose, currentScene.poses);
        // TODO play each pose -> shuffle list
        remainingPoseViews = currentPose.images.size();
        images = new MoodImages(currentPose.images);
    }

    private static Map<String, PictureSet> pictureSets(List<String> images) {
        Map<String, Pose> poses = new LinkedHashMap<>();

        for (String resource : images) {
            poses.computeIfAbsent(parent(resource), Pose::new).add(resource);
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
        return images.hasNext();
    }

    @Override
    public String next() {
        return images.next();
    }

    @Override
    public boolean contains(String resource) {
        return resources.contains(resource);
    }

    @Override
    public void hint(String... hint) {
        // TODO mood
        // TODO toys
        // TODO body state (slave standing or kneeling)
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
        final List<String> images;

        public Pose(String key) {
            this.key = key;
            this.images = new ArrayList<>();
        }

        public void add(String resource) {
            images.add(resource);
        }

        @Override
        public String toString() {
            return images.stream().map(s -> s.substring(key.length() + 1)).collect(toList()).toString();
        }

    }

}
