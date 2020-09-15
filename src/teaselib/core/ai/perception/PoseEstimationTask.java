package teaselib.core.ai.perception;

import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.Actor;
import teaselib.core.DeviceInteractionDefinitions;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.ai.TeaseLibAI;
import teaselib.core.ai.perception.HumanPose.HeadGestures;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.PoseAspect;
import teaselib.core.ai.perception.SceneCapture.EnclosureLocation;
import teaselib.core.events.EventSource;

class PoseEstimationTask implements Callable<PoseAspects> {
    final TeaseLibAI teaseLibAI;

    HumanPoseDeviceInteraction interactions;

    Lock awaitPose = new ReentrantLock();
    Condition poseChanged = awaitPose.newCondition();
    AtomicReference<PoseAspects> poseAspects = new AtomicReference<>(PoseAspects.Unavailable);

    PoseEstimationTask(HumanPoseDeviceInteraction interactions) {
        this.interactions = interactions;
        this.teaseLibAI = new TeaseLibAI();
    }

    public PoseAspects getPose(Interest interests) {
        // TODO set a flag to stop if aspects are a subset of desired aspects.
        // -> use future.get() to complete active estimation
        // restart the task (it's on the same thread) - it's already initialized

        // try {
        // return future.get();
        // } catch (InterruptedException e) {
        // Thread.currentThread().interrupt();
        // } catch (ExecutionException e) {
        // throw ExceptionUtil.asRuntimeException(e);
        // }

        return poseAspects.get();
    }

    // TODO replace the apsects with a condition a lä Requires.all(...), Requires.any(...)
    public boolean awaitPose(Interest interests, long duration, TimeUnit unit, PoseAspect... aspects) {
        PoseAspects result = poseAspects.get();

        if (result.containsAll(interests) && result.is(aspects)) {
            return true;
        }

        // TODO set interests, use current is match, or schedule pose estimation that matches temporary interests
        // - works for now as long as we just detect human presence as a single interest

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
    public PoseAspects call() throws InterruptedException {
        try {
            while (true) {
                SceneCapture device = awaitCaptureDevice();
                device.start();
                try {
                    try (HumanPose humanPose = new HumanPose();) {
                        while (!Thread.interrupted()) {
                            estimate(humanPose, device);
                        }
                    } catch (Exception e) {
                        HumanPoseDeviceInteraction.logger.error(e.getMessage(), e);
                    }
                } finally {
                    device.stop();
                }
            }
        } catch (Throwable e) {
            HumanPoseDeviceInteraction.logger.error(e.getMessage(), e);
            throw e;
        }
    }

    private SceneCapture awaitCaptureDevice() throws InterruptedException {
        List<SceneCapture> sceneCaptures;
        while (true) {
            sceneCaptures = teaseLibAI.sceneCaptures();
            if (sceneCaptures.isEmpty()) {
                Thread.sleep(5000);
            } else {
                Optional<SceneCapture> device = find(sceneCaptures, EnclosureLocation.External);
                if (device.isPresent()) {
                    return device.get();
                } else {
                    device = find(sceneCaptures, EnclosureLocation.Front);
                    if (device.isPresent()) {
                        return device.get();
                    }
                }
            }
        }
    }

    private static Optional<SceneCapture> find(List<SceneCapture> sceneCaptures, EnclosureLocation location) {
        return sceneCaptures.stream().filter(s -> s.location == location).findFirst();
    }

    private void estimate(HumanPose humanPose, SceneCapture device) throws InterruptedException {
        Actor actor = interactions.scriptRenderer.currentActor();
        if (actor != null) {
            AtomicBoolean updated = new AtomicBoolean(false);
            poseAspects.updateAndGet(previous -> {
                PoseAspects update = getPoseAspects(actor, humanPose, device);
                updated.set(!signal(actor, update, previous).isEmpty());
                return update;
            });

            synchronized (this) {
                if (updated.get()) {
                    ensureFrametimeMillis(humanPose, 200);
                } else if (poseAspects.get().is(HeadGestures.Gaze)) {
                    ensureFrametimeMillis(humanPose, 200);
                } else {
                    ensureFrametimeMillis(humanPose, 1000);
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

    private PoseAspects getPoseAspects(Actor actor, HumanPose humanPose, SceneCapture device) {
        Set<Interest> interests = interactions.definitions(actor).keySet();
        if (interests.isEmpty()) {
            return PoseAspects.Unavailable;
        } else {
            humanPose.setInterests(interests);
            List<HumanPose.Estimation> poses = humanPose.poses(device);
            if (poses.isEmpty()) {
                return PoseAspects.Unavailable;
            } else {
                return new PoseAspects(poses.get(0), interests);
            }
        }
    }

    private Set<Interest> signal(Actor actor, PoseAspects update, PoseAspects previous) {
        Set<Interest> updated = Collections.emptySet();

        if (theSameActor(actor)) {
            if (update.is(HumanPose.Status.Stream) || !update.equals(previous)) {
                DeviceInteractionDefinitions<Interest, EventSource<PoseEstimationEventArgs>> definitions = interactions
                        .definitions(actor);
                Set<Interest> interests = definitions.keySet();
                List<Map.Entry<Interest, EventSource<PoseEstimationEventArgs>>> reactions = definitions.stream()
                        .collect(toList());

                PoseEstimationEventArgs eventArgs = new PoseEstimationEventArgs(actor, update);
                reactions.stream().filter(e -> update.is(e.getKey())).map(Map.Entry::getValue)
                        .forEach(e -> e.fire(eventArgs));

                awaitPose.lock();
                try {
                    poseChanged.signalAll();
                } finally {
                    awaitPose.unlock();
                }

                updated = interactions.definitions(actor).keySet();
                updated.removeAll(interests);
            }
        }

        return updated;
    }

    private boolean theSameActor(Actor actor) {
        return actor == interactions.scriptRenderer.currentActor();
    }

}