package teaselib.core.ai.perception;

import static teaselib.core.util.ExceptionUtil.asRuntimeException;
import static teaselib.core.util.ExceptionUtil.reduce;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.core.Closeable;
import teaselib.core.DeviceInteractionDefinitions;
import teaselib.core.DeviceInteractionImplementation;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.ScriptRenderer;
import teaselib.core.ai.perception.HumanPose.Estimation;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.PoseAspect;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.events.Event;
import teaselib.core.events.EventSource;

public class HumanPoseDeviceInteraction extends
        DeviceInteractionImplementation<HumanPose.Interest, EventSource<PoseEstimationEventArgs>> implements Closeable {
    static final Logger logger = LoggerFactory.getLogger(HumanPoseDeviceInteraction.class);

    private final NamedExecutorService executor;
    private final PoseEstimationTask sceneCaptureTask;
    private Future<PoseAspects> future = null;
    final ScriptRenderer scriptRenderer;
    final HumanPose estimateImage;

    public abstract static class EventListener implements Event<PoseEstimationEventArgs> {
        final Interest interest;

        public EventListener(Interest interest) {
            this.interest = interest;
        }
    }

    public HumanPoseDeviceInteraction(ScriptRenderer scriptRenderer) {
        super(Object::equals);
        this.scriptRenderer = scriptRenderer;

        executor = NamedExecutorService.sameThread("Pose Estimation");
        sceneCaptureTask = new PoseEstimationTask(this, () -> executor.getQueue().isEmpty());
        estimateImage = submitAndGet(() -> getModel(HumanPose.Interest.Status));

        future = executor.submit(sceneCaptureTask);
    }

    public HumanPose getModel(Interest interest) {
        return sceneCaptureTask.teaseLibAI.getModel(interest);
    }

    @Override
    public void close() {
        if (future != null && !future.isDone() && !future.isCancelled()) {
            future.cancel(true);
        } else {
            submitAndGet(sceneCaptureTask::close);
        }
        executor.shutdown();
    }

    public PoseAspects getPose(Interest interests) {
        return sceneCaptureTask.getPose(interests);
    }

    public PoseAspects getPose(Interest interest, byte[] image) {
        Callable<PoseAspects> poseAspects = () -> {
            List<Estimation> poses = estimateImage.poses(image);
            if (poses.isEmpty()) {
                return PoseAspects.Unavailable;
            } else {
                return new PoseAspects(poses.get(0), Collections.singleton(interest));
            }
        };

        try {
            return submitAndGet(poseAspects);
        } finally {
            future = executor.submit(sceneCaptureTask);
        }
    }

    private void submitAndGet(Runnable task) {
        try {
            executor.submit(task).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        } catch (ExecutionException e) {
            throw asRuntimeException(reduce(e));
        }
    }

    private <T> T submitAndGet(Callable<T> task) {
        try {
            return executor.submit(task).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        } catch (ExecutionException e) {
            throw asRuntimeException(reduce(e));
        }
    }

    public boolean awaitPose(Interest interests, long duration, TimeUnit unit, PoseAspect... aspects) {
        return sceneCaptureTask.awaitPose(interests, duration, unit, aspects);
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
            listener.run(new PoseEstimationEventArgs(actor, sceneCaptureTask.getPose(listener.interest)));
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
