package teaselib.core.ai.perception;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.core.Closeable;
import teaselib.core.DeviceInteractionDefinitions;
import teaselib.core.DeviceInteractionImplementation;
import teaselib.core.ScriptRenderer;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.PoseAspect;
import teaselib.core.ai.perception.HumanPoseDeviceInteraction.Reaction;
import teaselib.core.concurrency.NamedExecutorService;

public class HumanPoseDeviceInteraction extends DeviceInteractionImplementation<HumanPose.Interest, Reaction>
        implements Closeable {
    static final Logger logger = LoggerFactory.getLogger(HumanPoseDeviceInteraction.class);

    private final NamedExecutorService poseEstimation;
    private final PoseEstimationTask task;
    private Future<PoseAspects> future = null;
    final ScriptRenderer scriptRenderer;

    public static class Reaction {
        final Interest aspect;
        final Consumer<PoseAspects> consumer;

        public Reaction(Interest aspect, Consumer<PoseAspects> consumer) {
            this.aspect = aspect;
            this.consumer = consumer;
        }
    }

    public HumanPoseDeviceInteraction(ScriptRenderer scriptRenderer) {
        super(Object::equals);
        this.scriptRenderer = scriptRenderer;

        poseEstimation = NamedExecutorService.sameThread("Pose Estimation");
        task = new PoseEstimationTask(this);
        future = poseEstimation.submit(task);
    }

    @Override
    public void close() {
        future.cancel(true);
        poseEstimation.shutdown();
    }

    public PoseAspects getPose(Interest interests) {
        return task.getPose(interests);
    }

    public boolean awaitPose(Interest interests, long duration, TimeUnit unit, PoseAspect... aspects) {
        return task.awaitPose(interests, duration, unit, aspects);
    }

    DeviceInteractionDefinitions<Interest, Reaction> defs(Actor actor) {
        return super.definitions(actor);
    }

    public HumanPose newHumanPose() {
        return task.teaseLibAI.newHumanPose();
    }

}
