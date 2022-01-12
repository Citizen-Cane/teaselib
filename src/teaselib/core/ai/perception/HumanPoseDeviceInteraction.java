package teaselib.core.ai.perception;

import static teaselib.core.util.ExceptionUtil.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.core.Closeable;
import teaselib.core.DeviceInteractionDefinitions;
import teaselib.core.DeviceInteractionImplementation;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.ScriptRenderer;
import teaselib.core.TeaseLib;
import teaselib.core.ai.TeaseLibAI;
import teaselib.core.ai.perception.HumanPose.Estimation;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.PoseAspect;
import teaselib.core.events.Event;
import teaselib.core.events.EventSource;

public class HumanPoseDeviceInteraction extends
        DeviceInteractionImplementation<HumanPose.Interest, EventSource<PoseEstimationEventArgs>> implements Closeable {
    static final Logger logger = LoggerFactory.getLogger(HumanPoseDeviceInteraction.class);

    final ScriptRenderer scriptRenderer;

    public final HumanPoseDeviceInteraction.EventListener proximitySensor;
    private final PoseEstimationTask poseEstimationTask;

    public abstract static class EventListener implements Event<PoseEstimationEventArgs> {
        final Set<Interest> interests;

        protected EventListener(Interest interest) {
            this(Collections.singleton(interest));
        }

        protected EventListener(Set<Interest> interests) {
            this.interests = interests;
        }
    }

    public HumanPoseDeviceInteraction(TeaseLib teaseLib, TeaseLibAI teaseLibAI, ScriptRenderer scriptRenderer)
            throws InterruptedException {
        super(Object::equals);
        this.scriptRenderer = scriptRenderer;
        this.poseEstimationTask = new PoseEstimationTask(teaseLibAI, this);
        this.proximitySensor = new ProximitySensor(teaseLib, Interest.asSet(Interest.Status, Interest.Proximity));
    }

    @Override
    public void close() {
        poseEstimationTask.close();

    }

    public boolean isActive() {
        return poseEstimationTask.isActive();
    }

    PoseAspects getPose(Interest interest) {
        return getPose(Collections.singleton(interest));
    }

    PoseAspects getPose(Set<Interest> interests) {
        return poseEstimationTask.getPose(interests);
    }

    PoseAspects getPose(Set<Interest> interests, byte[] image) throws InterruptedException {
        Callable<PoseAspects> poseAspects = () -> {
            HumanPose model = getModel(interests);
            List<Estimation> poses = model.poses(image);
            if (poses.isEmpty()) {
                return PoseAspects.Unavailable;
            } else {
                return new PoseAspects(poses.get(0), interests);
            }
        };
        return poseEstimationTask.submitAndGet(poseAspects);
    }

    private HumanPose getModel(Set<Interest> interests) throws InterruptedException {
        return poseEstimationTask.getModel(interests);
    }

    public void setPause(Runnable task) {
        poseEstimationTask.setPause(task);
    }

    public boolean awaitPose(Set<Interest> interests, long duration, TimeUnit unit, PoseAspect... aspects) {
        try {
            return poseEstimationTask.awaitPose(interests, duration, unit, aspects);
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException(e);
        }
    }

    @Override
    public DeviceInteractionDefinitions<Interest, EventSource<PoseEstimationEventArgs>> definitions(Actor actor) {
        synchronized (this) {
            return new DeviceInteractionDefinitions<>(super.definitions(actor), entry -> !entry.getValue().isEmpty());
        }
    }

    public void addEventListener(Actor actor, EventListener listener) {
        synchronized (this) {
            DeviceInteractionDefinitions<Interest, EventSource<PoseEstimationEventArgs>> definitions = super.definitions(
                    actor);
            for (Interest interest : listener.interests) {
                EventSource<PoseEstimationEventArgs> eventSource = definitions.get(interest,
                        k -> new EventSource<>(k.toString()));
                eventSource.add(listener);
            }
        }

        try {
            listener.run(new PoseEstimationEventArgs(actor, poseEstimationTask.getPose(listener.interests),
                    System.currentTimeMillis()));
        } catch (Exception e) {
            throw asRuntimeException(e);
        }
    }

    public boolean containsEventListener(Actor actor, EventListener listener) {
        EventSource<PoseEstimationEventArgs> eventSource;
        synchronized (this) {
            DeviceInteractionDefinitions<Interest, EventSource<PoseEstimationEventArgs>> definitions = super.definitions(
                    actor);
            for (Interest interest : listener.interests) {
                eventSource = definitions.get(interest);
                if (eventSource != null && eventSource.contains(listener)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void removeEventListener(Actor actor, EventListener listener) {
        synchronized (this) {
            DeviceInteractionDefinitions<Interest, EventSource<PoseEstimationEventArgs>> definitions = super.definitions(
                    actor);
            for (Interest interest : listener.interests) {
                EventSource<PoseEstimationEventArgs> eventSource = definitions.get(interest);
                eventSource.remove(listener);
            }
        }
    }

}
