package teaselib.core;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import teaselib.Images;
import teaselib.Resources;
import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Estimation;
import teaselib.core.ai.perception.HumanPoseScriptInteraction;
import teaselib.core.util.Prefetcher;
import teaselib.util.AnnotatedImage;

public abstract class AbstractImages implements Images {

    protected final Resources resources;
    private final Prefetcher<AnnotatedImage> imageFetcher;
    private final HumanPose humanPose;

    protected AbstractImages(Resources resources) {
        Objects.requireNonNull(resources);

        this.resources = resources;
        imageFetcher = new Prefetcher<>(resources.script.scriptRenderer.getPrefetchExecutorService(),
                this::annotatedImage);
        HumanPoseScriptInteraction interaction = resources.script.interaction(HumanPoseScriptInteraction.class);
        humanPose = interaction.newHumanPose();
    }

    public Prefetcher<AnnotatedImage> prefetcher() {
        return imageFetcher;
    }

    @Override
    public AnnotatedImage annotated(String resource) throws IOException, InterruptedException {
        return imageFetcher.get(resource);
    }

    private AnnotatedImage annotatedImage(String resource) throws IOException {
        byte[] image = resources.getBytes(resource);
        AnnotatedImage annotatedImage;
        List<Estimation> results = humanPose.poses(image);
        if (results.isEmpty()) {
            annotatedImage = new AnnotatedImage(resource, image);
        } else {
            Estimation pose = results.get(0);
            annotatedImage = new AnnotatedImage(resource, image, pose);
        }
        return annotatedImage;
    }

}