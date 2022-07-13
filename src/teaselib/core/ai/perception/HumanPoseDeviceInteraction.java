package teaselib.core.ai.perception;

import static teaselib.core.util.ExceptionUtil.*;

import java.util.ArrayList;
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
import teaselib.core.ScriptEventArgs.ActorChanged;
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

    private final PoseEstimationTask poseEstimationTask;
    public final ProximitySensor proximitySensor;
    final ScriptRenderer scriptRenderer;
    final Event<ActorChanged> actorChangedListener;

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
        this.poseEstimationTask = new PoseEstimationTask(teaseLibAI, this);
        this.proximitySensor = new ProximitySensor(teaseLib, HumanPose.asSet(Interest.Status, Interest.Proximity));
        this.scriptRenderer = scriptRenderer;
        this.actorChangedListener = e -> poseEstimationTask.setActor(e.actor);
        scriptRenderer.events.actorChanged.add(actorChangedListener);
    }

    @Override
    public void close() {
        scriptRenderer.events.actorChanged.remove(actorChangedListener);
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

    public PoseAspects getPose(Set<Interest> interests, byte[] image) throws InterruptedException {
        Callable<PoseAspects> poseAspects = () -> {
            long timestamp = System.currentTimeMillis();
            HumanPose model = getModel(interests);
            model.setInterests(interests);
            List<Estimation> poses = model.poses(image);
            if (poses.isEmpty()) {
                return PoseAspects.Unavailable;
            } else {
                return new PoseAspects(poses.get(0), timestamp, interests);
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

    public void clearPause() {
        poseEstimationTask.clearPause();
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
        List<EventSource<PoseEstimationEventArgs>> eventSources = new ArrayList<>();
        synchronized (this) {
            DeviceInteractionDefinitions<Interest, EventSource<PoseEstimationEventArgs>> definitions = super.definitions(
                    actor);
            for (Interest interest : listener.interests) {
                EventSource<PoseEstimationEventArgs> eventSource = definitions.get(interest,
                        k -> new EventSource<>(k.toString()));
                eventSources.add(eventSource);
            }
        }
        eventSources.stream().forEach(eventSource -> eventSource.add(listener));
        poseEstimationTask.interestChanged();

        try {
            PoseEstimationEventArgs eventArgs = new PoseEstimationEventArgs(actor,
                    poseEstimationTask.getPose(listener.interests));
            listener.run(eventArgs);
        } catch (Exception e) {
            throw asRuntimeException(e);
        }
    }

    public boolean containsEventListener(Actor actor, EventListener listener) {
        synchronized (this) {
            DeviceInteractionDefinitions<Interest, EventSource<PoseEstimationEventArgs>> definitions = super.definitions(
                    actor);
            for (Interest interest : listener.interests) {
                EventSource<PoseEstimationEventArgs> eventSource;
                eventSource = definitions.get(interest);
                if (eventSource != null && eventSource.contains(listener)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void removeEventListener(Actor actor, EventListener listener) {
        List<EventSource<PoseEstimationEventArgs>> eventSources = new ArrayList<>();
        synchronized (this) {
            DeviceInteractionDefinitions<Interest, EventSource<PoseEstimationEventArgs>> definitions = super.definitions(
                    actor);
            for (Interest interest : listener.interests) {
                EventSource<PoseEstimationEventArgs> eventSource = definitions.get(interest);
                eventSources.add(eventSource);
            }
        }
        eventSources.stream().forEach(eventSource -> eventSource.remove(listener));
        poseEstimationTask.interestChanged();
    }

}
