package teaselib.core.ai.perception;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;

import teaselib.Actor;
import teaselib.core.DeviceInteractionDefinitions;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.ai.TeaseLibAI;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.PoseAspect;
import teaselib.core.events.EventSource;

class PoseEstimationTask implements Callable<PoseAspects> {
    final TeaseLibAI teaseLibAI;
    final HumanPoseDeviceInteraction interaction;
    private final BooleanSupplier processFrame;

    private Lock awaitPose = new ReentrantLock();
    private Condition poseChanged = awaitPose.newCondition();
    private AtomicReference<PoseAspects> poseAspects = new AtomicReference<>(PoseAspects.Unavailable);
    private SceneCapture device = null;
    private HumanPose humanPose = null;

    PoseEstimationTask(HumanPoseDeviceInteraction interaction, BooleanSupplier processFrame) {
        this.interaction = interaction;
        this.teaseLibAI = new TeaseLibAI();
        this.processFrame = processFrame;
    }

    public PoseAspects getPose(Interest interest) {
        if (interest != Interest.Status) {
            throw new UnsupportedOperationException("TODO Match interests with pose estiamtion model");
        }
        return poseAspects.get();
    }

    // TODO replace the aspects with a condition a l Requires.all(...), Requires.any(...)
    public boolean awaitPose(Interest interests, long duration, TimeUnit unit, PoseAspect... aspects) {
        PoseAspects result = poseAspects.get();

        if (result.containsAll(interests) && result.is(aspects)) {
            return true;
        }

        try {
            awaitPose.lockInterruptibly();
            try {
                while (poseChanged.await(duration, unit)) {
                    result = poseAspects.get();
                    if (result.is(aspects)) {
                        return true;
                    }
                }
                return false;
            } finally {
                awaitPose.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException();
        }
    }

    @Override
    public PoseAspects call() {
        try {
            while (processFrame.getAsBoolean() && !Thread.interrupted()) {
                estimatePoses(device);
            }
            return poseAspects.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable e) {
            HumanPoseDeviceInteraction.logger.error(e.getMessage(), e);
            throw e;
        } finally {
            if (Thread.currentThread().isInterrupted()) {
                close();
            }
        }

        return PoseAspects.Unavailable;
    }

    private void estimatePoses(SceneCapture device) throws InterruptedException {
        if (device == null) {
            device = teaseLibAI.awaitCaptureDevice();
            device.start();
            humanPose = teaseLibAI.getModel(Interest.Status);
        }

        while (processFrame.getAsBoolean() && !Thread.interrupted()) {
            estimate(humanPose, device);
        }
    }

    private void estimate(HumanPose humanPose, SceneCapture device) throws InterruptedException {
        Actor actor = interaction.scriptRenderer.currentActor();
        if (actor != null) {
            AtomicBoolean updated = new AtomicBoolean(false);
            poseAspects.updateAndGet(previous -> {
                PoseAspects update = getPoseAspects(actor, humanPose, device, previous);
                updated.set(!signal(actor, update, previous).isEmpty());
                return update;
            });

            synchronized (this) {
                if (updated.get()) {
                    ensureFrametimeMillis(humanPose, 200);
                } else {
                    PoseAspects pose = poseAspects.get();
                    if (pose.is(Interest.HeadGestures)) {
                        ensureFrametimeMillis(humanPose, 200);
                    } else {
                        ensureFrametimeMillis(humanPose, 1000);
                    }
                }
            }
        } else {
            synchronized (this) {
                wait(1000);
            }
        }
    }

    private void ensureFrametimeMillis(HumanPose humanPose, long frameTimeMillis) throws InterruptedException {
        long now = System.currentTimeMillis();
        long timestamp = humanPose.getTimestamp();
        long estimationMillis = now - timestamp;
        long durationMillis = Math.max(0, frameTimeMillis - estimationMillis);
        if (durationMillis > 0) {
            wait(durationMillis);
        }
    }

    private PoseAspects getPoseAspects(Actor actor, HumanPose humanPose, SceneCapture device, PoseAspects previous) {
        Set<Interest> interests = interaction.definitions(actor).keySet();
        if (interests.isEmpty()) {
            return PoseAspects.Unavailable;
        } else {
            humanPose.setInterests(interests);
            List<HumanPose.Estimation> poses = humanPose.poses(device);
            if (poses.isEmpty()) {
                return PoseAspects.Unavailable;
            } else {
                return new PoseAspects(poses.get(0), interests, previous);
            }
        }
    }

    private Set<Interest> signal(Actor actor, PoseAspects update, PoseAspects previous) {
        Set<Interest> updated = Collections.emptySet();

        if (theSameActor(actor)) {
            if (update.is(HumanPose.Status.Stream) || !update.equals(previous)) {
                DeviceInteractionDefinitions<Interest, EventSource<PoseEstimationEventArgs>> definitions = interaction
                        .definitions(actor);
                Set<Interest> previousInterests = definitions.keySet();

                PoseEstimationEventArgs eventArgs = new PoseEstimationEventArgs(actor, update);
                definitions.stream().filter(e -> update.is(e.getKey())).map(Map.Entry::getValue)
                        .forEach(e -> e.fire(eventArgs));

                awaitPose.lock();
                try {
                    poseChanged.signalAll();
                } finally {
                    awaitPose.unlock();
                }

                updated = interaction.definitions(actor).keySet();
                updated.removeAll(previousInterests);
            }
        }

        return updated;
    }

    private boolean theSameActor(Actor actor) {
        return actor == interaction.scriptRenderer.currentActor();
    }

    public void close() {
        if (humanPose != null) {
            humanPose.close();
            humanPose = null;
        }
        if (device != null) {
            device.stop();
            device = null;
        }
    }

}
