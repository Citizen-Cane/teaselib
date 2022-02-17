package teaselib.core;

import java.io.IOException;
import java.nio.file.Paths;

import teaselib.ActorImages;
import teaselib.CachedImages;
import teaselib.Resources;
import teaselib.core.Host.Location;
import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Estimation;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.Status;
import teaselib.core.ai.perception.HumanPoseDeviceInteraction;
import teaselib.core.ai.perception.PoseAspects;
import teaselib.util.AnnotatedImage;

public abstract class AbstractImages extends CachedImages implements ActorImages {

    public final PoseCache poseCache;

    protected AbstractImages(Resources resources) {
        super(resources);
        this.poseCache = new PoseCache(
                Paths.get(resources.teaseLib.host.getLocation(Location.User).getAbsolutePath(), "Pose cache"),
                resources.loader, this::computePose);
    }

    private Estimation computePose(String resource, byte[] image) {
        // TODO add status display to host since this is not about actors and script conversation
        resources.teaseLib.host.showInterTitle("Pose estimation pre-caching: \n\n" + resource);
        resources.teaseLib.host.show();

        PoseAspects pose;
        try {
            // TODO implement HumanPose.Interest.Pose to get better results
            pose = resources.teaseLib.deviceInteraction(HumanPoseDeviceInteraction.class).getPose(Interest.Head, image);
            return pose.is(Status.Available) ? pose.estimation : HumanPose.Estimation.NONE;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return HumanPose.Estimation.NONE;
        }
    }

    @Override
    protected AnnotatedImage annotatedImage(String resource) throws IOException {
        byte[] image = resources.getBytes(resource);
        return poseCache.annotatedImage(resource, image);
    }

    @Override
    public boolean hasNext() {
        return !resources.isEmpty();
    }

}
