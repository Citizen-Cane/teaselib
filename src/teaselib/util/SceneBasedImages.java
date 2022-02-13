package teaselib.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Resources;
import teaselib.core.Script;
import teaselib.core.events.EventArgs;
import teaselib.core.util.ExceptionUtil;
import teaselib.util.PictureSetAssets.Take;

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

    private final Script script;
    private final PictureSetAssets pictureSets;

    public SceneBasedImages(Resources resources) {
        this.script = resources.script;
        this.pictureSets = new PictureSetAssets(resources);

        if (pictureSets.isEmpty()) {
            String any = "";
            currentTake = new PictureSetAssets.Take(any, resources.script);
            currentPose = new PictureSetAssets.Pose(any);
            currentPose.add(currentTake);
            currentScene = new PictureSetAssets.Scene(any);
            currentScene.add(currentPose);
            currentSet = new PictureSetAssets.PictureSet(any);
            currentSet.add(currentScene);
        } else {
            chooseSet();
            loadPoses();
            resources.script.scriptRenderer.events.afterPrompt.add(this::handleAfterChoices);
            resources.script.scriptRenderer.events.beforeMessage.add(this::handleBeforeMessage);
            // TODO remove events when instance are not used anymore - otherwise may result in memory leak
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
            script.scriptRenderer.getPrefetchExecutorService().submit(() -> {
                logger.info("Pose computation complete");
            }).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw ExceptionUtil.asRuntimeException(e);
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
        currentSet = script.random.item(currentSet, new ArrayList<>(pictureSets.values()));
        // TODO play each scene -> shuffle list
        remainingSetScenes = currentSet.assets.size();
        chooseScene();
    }

    private void chooseScene() {
        // TODO random / linear
        // TODO intro outro
        currentScene = script.random.item(currentScene, currentSet.assets);
        // TODO play each scene -> shuffle list
        remainingScenePoses = currentScene.assets.size();
        choosePose();
    }

    void choosePose() {
        // TODO random / linear
        currentPose = script.random.item(currentPose, currentScene.assets);
        // TODO play each pose -> shuffle list
        remainingPoseTakes = currentPose.assets.size();
        chooseTake();
    }

    void chooseTake() {
        // TODO random / linear
        currentTake = script.random.item(currentTake, currentPose.assets);
        // TODO play each pose -> shuffle list
        remainingTakeViews = currentTake.assets.size();
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
    public String toString() {
        return pictureSets.toString();
    }
}
