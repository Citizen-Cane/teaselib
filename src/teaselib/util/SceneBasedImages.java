package teaselib.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Resources;
import teaselib.core.Script;
import teaselib.core.util.ExceptionUtil;
import teaselib.util.PictureSetAssets.Attribute;
import teaselib.util.PictureSetAssets.Take;
import teaselib.util.math.Random;

public class SceneBasedImages implements teaselib.ActorImages {

    static final Logger logger = LoggerFactory.getLogger(Script.class);

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

    private PictureSetAssets.PictureSet currentSet;
    private PictureSetAssets.Scene currentScene;
    private PictureSetAssets.Pose currentPose;
    private PictureSetAssets.Take currentTake;

    private PictureSetAssets.Assets.LinearDirection linearDirection = PictureSetAssets.Assets.LinearDirection.Forward;

    private final PictureSetAssets pictureSets;
    private final ExecutorService prefetch;
    private final Random random;

    public SceneBasedImages(Resources resources) {
        this.pictureSets = new PictureSetAssets(resources);
        this.prefetch = resources.prefetch;
        this.random = new Random();

        if (pictureSets.isEmpty()) {
            String any = "";
            currentTake = new PictureSetAssets.Take(any, resources);
            currentPose = new PictureSetAssets.Pose(any);
            currentPose.add(currentTake);
            currentScene = new PictureSetAssets.Scene(any);
            currentScene.add(currentPose);
            currentSet = new PictureSetAssets.PictureSet(any);
            currentSet.add(currentScene);
        } else {
            chooseSet();
            loadPoses();
        }
    }

    private void loadPoses() {
        var fetch = pictureSets.resources().filter(SceneBasedImages::loaded).toList();
        if (fetch.size() > 0) {
            logger.info("Computing poses for {} images", fetch.size());
            fetch.forEach(this::fetch);
            awaitPosesComputed();
        }
    }

    private static boolean loaded(Entry<String, Take> entry) {
        return !entry.getValue().images.poseCache.loadPose(entry.getKey());
    }

    private void fetch(Entry<String, Take> entry) {
        entry.getValue().images.fetch(entry.getKey());
    }

    private void awaitPosesComputed() {
        try {
            prefetch.submit(() -> {
                logger.info("Pose computation complete");
            }).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    private void decrementRemainingViews() {
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

    private void chooseFollowupPictures() {
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
        // TODO persistence (choose set once per session)
        currentSet = random.item(currentSet, new ArrayList<>(pictureSets.values()));
        // TODO play each scene -> shuffle list
        remainingSetScenes = currentSet.assets.size();
        chooseScene();
    }

    private void chooseScene() {
        // TODO intro outro
        if (currentSet.attributes.contains(Attribute.Linear)) {
            var newScene = currentSet.successorOf(currentScene, linearDirection);
            if (newScene == null) {
                chooseSet();
                return;
            } else {
                currentScene = newScene;
            }
        } else {
            boolean wasLinear = currentScene != null && currentScene.attributes.contains(Attribute.Linear);
            var newScene = random.item(currentScene, currentSet.assets);
            if (newScene == currentScene) {
                chooseSet();
                return;
            }
            boolean isLinear = newScene.attributes.contains(Attribute.Linear);
            if (wasLinear && isLinear) {
                linearDirection = linearDirection.opposite();
            }
            currentScene = newScene;
        }
        remainingScenePoses = currentScene.assets.size();
        choosePose();
    }

    void choosePose() {
        if (currentScene.attributes.contains(Attribute.Linear)) {
            var newPose = currentScene.successorOf(currentPose, linearDirection);
            if (newPose == null) {
                chooseScene();
                return;
            } else {
                currentPose = newPose;
            }
        } else {
            boolean wasLinear = currentPose != null && currentPose.attributes.contains(Attribute.Linear);
            var newPose = random.item(currentPose, currentScene.assets);
            if (newPose == currentPose) {
                chooseScene();
                return;
            }
            boolean isLinear = newPose.attributes.contains(Attribute.Linear);
            if (wasLinear && isLinear) {
                linearDirection = linearDirection.opposite();
            }
            currentPose = newPose;
        }
        remainingPoseTakes = currentPose.assets.size();
        chooseTake();
    }

    void chooseTake() {
        if (currentPose.attributes.contains(Attribute.Linear)) {
            var newTake = currentPose.successorOf(currentTake, linearDirection);
            if (newTake == null) {
                choosePose();
                return;
            } else {
                currentTake = newTake;
            }
        } else {
            boolean wasLinear = currentTake != null && currentTake.attributes.contains(Attribute.Linear);
            var newTake = random.item(currentTake, currentPose.assets);
            if (newTake == currentTake) {
                // TODO infinite loop when no images present, or only one set
                choosePose();
                return;
            } else if (!newTake.images.hasNext()) {
                // TODO honor once here and choose a take/pose/scene that has not been used yet
                // -> wben all done, choose a different random top-level entity -> choosePose() ...
                newTake.images.advance(Next.Take); // Workaround for resetting "Once"
            }
            boolean isLinear = newTake.attributes.contains(Attribute.Linear);
            if (wasLinear && isLinear) {
                linearDirection = linearDirection.opposite();
            }
            currentTake = newTake;
        }
        remainingTakeViews = currentTake.assets.size();
    }

    @Override
    public boolean hasNext() {
        return currentTake.images.hasNext();
    }

    @Override
    public String next(String... hints) {
        var all = new ArrayList<String>(hints.length + 1);
        all.addAll(Arrays.asList(hints));
        all.add(linearDirection.name());
        var allHints = new String[all.size()];
        return currentTake.images.next(all.toArray(allHints));
    }

    @Override
    public boolean contains(String resource) {
        return pictureSets.contains(resource);
    }

    @Override
    public void fetch(String resource) {
        pictureSets.fetch(resource);
    }

    @Override
    public AnnotatedImage annotated(String resource) throws IOException, InterruptedException {
        return pictureSets.annotated(resource);
    }

    @Override
    public void advance(Next pictures, String... hints) {
        switch (pictures) {
        case Message:
            decrementRemainingViews();
            break;
        case Section:
            chooseFollowupPictures();
            break;
        case Take:
            chooseTake();
            break;
        case Pose:
            choosePose();
            break;
        case Scene:
            chooseScene();
            break;
        case Set:
            chooseSet();
            break;
        default:
            throw new UnsupportedOperationException(pictures.toString());
        }
        // Ignore
    }

    @Override
    public String toString() {
        return pictureSets.toString();
    }
}
