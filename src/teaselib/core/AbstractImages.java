package teaselib.core;

import java.io.IOException;
import java.util.Objects;

import teaselib.Images;
import teaselib.Resources;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.Status;
import teaselib.core.ai.perception.HumanPoseScriptInteraction;
import teaselib.core.ai.perception.PoseAspects;
import teaselib.core.util.Prefetcher;
import teaselib.util.AnnotatedImage;

public abstract class AbstractImages implements Images {

    protected final Resources resources;
    private final Prefetcher<AnnotatedImage> imageFetcher;
    private final HumanPoseScriptInteraction interaction;

    protected AbstractImages(Resources resources) {
        Objects.requireNonNull(resources);

        this.resources = resources;
        this.imageFetcher = new Prefetcher<>(resources.script.scriptRenderer.getPrefetchExecutorService(),
                this::annotatedImage);
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
        AnnotatedImage annotatedImage;
        PoseAspects pose = interaction.getPose(Interest.Proximity, image);
        if (pose.is(Status.Available)) {
            annotatedImage = new AnnotatedImage(resource, image, pose.estimation);
        } else {
            annotatedImage = new AnnotatedImage(resource, image);
        }
        return annotatedImage;
    }

}
