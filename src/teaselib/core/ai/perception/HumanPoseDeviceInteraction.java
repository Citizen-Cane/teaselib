package teaselib.core.ai.perception;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.core.Closeable;
import teaselib.core.DeviceInteractionDefinitions;
import teaselib.core.DeviceInteractionImplementation;
import teaselib.core.ScriptRenderer;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.PoseAspect;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.events.Event;
import teaselib.core.events.EventSource;

public class HumanPoseDeviceInteraction extends
        DeviceInteractionImplementation<HumanPose.Interest, EventSource<PoseEstimationEventArgs>> implements Closeable {
    static final Logger logger = LoggerFactory.getLogger(HumanPoseDeviceInteraction.class);

    private final NamedExecutorService poseEstimation;
    private final PoseEstimationTask task;
    private Future<PoseAspects> future = null;
    final ScriptRenderer scriptRenderer;

    public abstract static class EventListener implements Event<PoseEstimationEventArgs> {
        final Interest interest;

        public EventListener(Interest interest) {
            this.interest = interest;
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

    @Override
    public DeviceInteractionDefinitions<Interest, EventSource<PoseEstimationEventArgs>> definitions(Actor actor) {
        DeviceInteractionDefinitions<Interest, EventSource<PoseEstimationEventArgs>> all = super.definitions(actor);
        return new DeviceInteractionDefinitions<>(all, entry -> !entry.getValue().isEmpty());
    }

    public void addEventListener(Actor actor, EventListener listener) {
        DeviceInteractionDefinitions<Interest, EventSource<PoseEstimationEventArgs>> definitions = super.definitions(
                actor);
        EventSource<PoseEstimationEventArgs> eventSource = definitions.get(listener.interest,
                k -> new EventSource<>(k.toString()));
        eventSource.add(listener);
    }

    public boolean containsEventListener(Actor actor, EventListener listener) {
        DeviceInteractionDefinitions<Interest, EventSource<PoseEstimationEventArgs>> definitions = super.definitions(
                actor);
        EventSource<PoseEstimationEventArgs> eventSource = definitions.get(listener.interest);
        return eventSource != null && eventSource.contains(listener);
    }

    public void removeEventListener(Actor actor, EventListener listener) {
        DeviceInteractionDefinitions<Interest, EventSource<PoseEstimationEventArgs>> definitions = super.definitions(
                actor);
        EventSource<PoseEstimationEventArgs> eventSource = definitions.get(listener.interest);
        eventSource.remove(listener);
    }

    public HumanPose newHumanPose() {
        return task.teaseLibAI.newHumanPose();
    }

}
