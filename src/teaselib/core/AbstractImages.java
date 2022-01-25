package teaselib.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import teaselib.Images;
import teaselib.Resources;
import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Estimation;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.Status;
import teaselib.core.ai.perception.HumanPoseScriptInteraction;
import teaselib.core.util.Prefetcher;
import teaselib.util.AnnotatedImage;

public abstract class AbstractImages implements Images {

    protected final Resources resources;
    private final Prefetcher<AnnotatedImage> imageFetcher;
    private final HumanPoseScriptInteraction interaction;

    // TODO prefetch from xml to avoid computing poses at each startup
    private final Map<String, HumanPose.Estimation> poses = new HashMap<>();

    private final Map<String, AnnotatedImage> annotatedImages = new HashMap<>();

    protected AbstractImages(Resources resources) {
        Objects.requireNonNull(resources);

        this.resources = resources;
        this.imageFetcher = new Prefetcher<>(resources.script.scriptRenderer.getPrefetchExecutorService(),
                this::annotatedImage);
        resources.stream().forEach(imageFetcher::fetch);
        this.interaction = resources.script.interaction(HumanPoseScriptInteraction.class);
    }

    @Override
    public boolean hasNext() {
        return !resources.isEmpty();
    }

    @Override
    public boolean contains(String resource) {
        return resources.contains(resource);
    }

    @Override
    public void fetch(String resource) {
        imageFetcher.fetch(resource);
    }

    @Override
    public AnnotatedImage annotated(String resource) throws IOException, InterruptedException {
        return imageFetcher.get(resource);
    }

    private AnnotatedImage annotatedImage(String resource) throws IOException {
        byte[] image = resources.getBytes(resource);
        return annotatedImages.computeIfAbsent(resource, key -> {
            HumanPose.Estimation estimation = poses.computeIfAbsent(resource, k -> {
                return computePose(image);
            });
            return new AnnotatedImage(key, image, estimation);
        });
    }

    private Estimation computePose(byte[] image) {
        // TODO should be torso (upper body & arms)
        var pose = interaction.getPose(Interest.Proximity, image);
        return pose.is(Status.Available) ? pose.estimation : HumanPose.Estimation.NONE;
    }

}
