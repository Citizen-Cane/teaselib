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
        if (interest != Interest.Status && interest != Interest.Proximity && interest != Interest.HeadGestures) {
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
            while (canProcessNextFrame()) {
                estimatePoses();
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

    private void estimatePoses() throws InterruptedException {
        while (canProcessNextFrame()) {
            if (device == null) {
                device = teaseLibAI.awaitCaptureDevice();
                device.start();
                humanPose = teaseLibAI.getModel(Interest.Status);
            }
            try {
                while (canProcessNextFrame()) {
                    estimate();
                }
            } catch (SceneCapture.DeviceLost e) {
                HumanPoseDeviceInteraction.logger.warn(e.getMessage());
                try {
                    synchronized (this) {
                        wait(10000);
                    }
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void estimate() throws InterruptedException {
        Actor actor = interaction.scriptRenderer.currentActor();
        if (actor != null) {
            AtomicBoolean updated = new AtomicBoolean(false);
            poseAspects.updateAndGet(previous -> {
                PoseAspects update = getPoseAspects(actor, previous);
                updated.set(!signal(actor, update, previous).isEmpty());
                return update;
            });

            synchronized (this) {
                if (updated.get()) {
                    ensureFrametimeMillis(200);
                } else {
                    PoseAspects pose = poseAspects.get();
                    if (pose.is(Interest.HeadGestures)) {
                        ensureFrametimeMillis(200);
                    } else {
                        ensureFrametimeMillis(1000);
                    }
                }
            }
        } else {
            synchronized (this) {
                sleep(1000);
            }
        }
    }

    private void ensureFrametimeMillis(long frameTimeMillis) throws InterruptedException {
        long now = System.currentTimeMillis();
        long timestamp = humanPose.getTimestamp();
        long estimationMillis = now - timestamp;
        long durationMillis = Math.max(0, frameTimeMillis - estimationMillis);
        if (durationMillis > 0) {
            sleep(durationMillis);
        }
    }

    private void sleep(long durationMillis) throws InterruptedException {
        if (canProcessNextFrame()) {
            wait(durationMillis);
        }
    }

    private boolean canProcessNextFrame() {
        return processFrame.getAsBoolean() && !Thread.interrupted();
    }

    private PoseAspects getPoseAspects(Actor actor, PoseAspects previous) {
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
            // TODO fire only stream-relevant interests (Gaze), but not proximity
            // - stream relevant aspects are fired always, change relevant only when they've changed
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
