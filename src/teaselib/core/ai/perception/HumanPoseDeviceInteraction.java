package teaselib.core.ai.perception;

import static teaselib.core.util.ExceptionUtil.asRuntimeException;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.core.Closeable;
import teaselib.core.DeviceInteractionDefinitions;
import teaselib.core.DeviceInteractionImplementation;
import teaselib.core.ScriptRenderer;
import teaselib.core.ai.TeaseLibAI;
import teaselib.core.ai.perception.HumanPose.Estimation;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.PoseAspect;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.events.Event;
import teaselib.core.events.EventSource;

public class HumanPoseDeviceInteraction extends
        DeviceInteractionImplementation<HumanPose.Interest, EventSource<PoseEstimationEventArgs>> implements Closeable {
    static final Logger logger = LoggerFactory.getLogger(HumanPoseDeviceInteraction.class);

    private final NamedExecutorService taskExecutor;
    final ScriptRenderer scriptRenderer;

    private final PoseEstimationTask poseEstimationTask;
    private Future<PoseAspects> future = null;

    public abstract static class EventListener implements Event<PoseEstimationEventArgs> {
        final Interest interest;

        protected EventListener(Interest interest) {
            this.interest = interest;
        }
    }

    public HumanPoseDeviceInteraction(TeaseLibAI teaseLibAI, ScriptRenderer scriptRenderer) {
        super(Object::equals);
        this.scriptRenderer = scriptRenderer;
        this.taskExecutor = NamedExecutorService.sameThread("Pose Estimation");
        this.poseEstimationTask = new PoseEstimationTask(teaseLibAI, this);
        future = taskExecutor.submit(poseEstimationTask);
    }

    @Override
    public void close() {
        if (future != null && !future.isDone() && !future.isCancelled()) {
            future.cancel(true);
        }
        taskExecutor.shutdown();
        try {
            taskExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public PoseAspects getPose(Interest interests) {
        return poseEstimationTask.getPose(interests);
    }

    public PoseAspects getPose(Interest interest, byte[] image) {
        Callable<PoseAspects> poseAspects = () -> {
            HumanPose model = getModel(Interest.Status);
            List<Estimation> poses = model.poses(image);
            if (poses.isEmpty()) {
                return PoseAspects.Unavailable;
            } else {
                return new PoseAspects(poses.get(0), Collections.singleton(interest));
            }
        };
        return poseEstimationTask.submitAndGetResult(poseAspects);
    }

    private HumanPose getModel(Interest interest) {
        return poseEstimationTask.getModel(interest);
    }

    public void setPause(Runnable task) {
        poseEstimationTask.setPause(task);
    }

    public boolean awaitPose(Interest interests, long duration, TimeUnit unit, PoseAspect... aspects) {
        return poseEstimationTask.awaitPose(interests, duration, unit, aspects);
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

        try {
            listener.run(new PoseEstimationEventArgs(actor, poseEstimationTask.getPose(listener.interest),
                    System.currentTimeMillis()));
        } catch (Exception e) {
            throw asRuntimeException(e);
        }
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

}
