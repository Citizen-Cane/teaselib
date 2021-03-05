package teaselib.core.ai.perception;

import static teaselib.core.util.ExceptionUtil.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.core.DeviceInteractionDefinitions;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.ai.TeaseLibAI;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.PoseAspect;
import teaselib.core.events.EventSource;
import teaselib.core.util.ExceptionUtil;

class PoseEstimationTask implements Callable<PoseAspects> {
    static final Logger logger = LoggerFactory.getLogger(PoseEstimationTask.class);

    private static final Runnable Noop = () -> {
    };

    private final TeaseLibAI teaseLibAI;
    private final HumanPoseDeviceInteraction interaction;
    private final ExecutorService inferenceExecutor;
    private final Thread inferenceThread;
    private final Lock awaitPose = new ReentrantLock();
    private final Condition poseChanged = awaitPose.newCondition();
    private final AtomicReference<PoseAspects> poseAspects = new AtomicReference<>(PoseAspects.Unavailable);
    private final ReentrantLock estimatePoses = new ReentrantLock();
    private final AtomicReference<Runnable> pause = new AtomicReference<>(Noop);
    private SceneCapture device = null;
    private HumanPose humanPose = null;

    PoseEstimationTask(TeaseLibAI teaseLibAI, HumanPoseDeviceInteraction humanPoseDeviceInteraction) {
        this.teaseLibAI = teaseLibAI;
        this.interaction = humanPoseDeviceInteraction;
        this.inferenceExecutor = teaseLibAI.getExecutor(TeaseLibAI.ExecutionType.Accelerated);
        this.inferenceThread = submitAndGetResult(Thread::currentThread);
    }

    public PoseAspects getPose(Interest interest) {
        throwIfUnsupported(interest);
        return poseAspects.get();
    }

    private void throwIfUnsupported(Interest interest) {
        if (interest != Interest.Status && interest != Interest.Proximity && interest != Interest.HeadGestures) {
            // TODO manage multiple models (e.g. mobilenet_thin & CMU)
            throw new UnsupportedOperationException(
                    "TODO provide multiple pose estimation models to match all interests");
        }
    }

    // TODO replace the aspects with a condition a l Requires.all(...), Requires.any(...)
    boolean awaitPose(Interest interests, long duration, TimeUnit unit, PoseAspect... aspects) {
        throwIfUnsupported(interests);
        PoseAspects result = poseAspects.get();
        if (result.containsAll(interests) && result.is(aspects)) {
            return true;
        } else {
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
    }

    HumanPose getModel(Interest interest) {
        throwIfUnsupported(interest);
        if (humanPose == null) {
            if (Thread.currentThread() == this.inferenceThread) {
                humanPose = teaseLibAI.getModel(interest);
            } else {
                humanPose = submitAndGetResult(() -> teaseLibAI.getModel(interest));
            }
        }
        return humanPose;
    }

    <T> T submitAndGetResult(Callable<T> task) {
        try {
            return submit(task).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        } catch (ExecutionException e) {
            throw asRuntimeException(reduce(e));
        }
    }

    private Future<?> submit(Runnable task) {
        return inferenceExecutor.submit(task::run);
    }

    private <T> Future<T> submit(Callable<T> task) {
        return inferenceExecutor.submit(task::call);
    }

    @Override
    public PoseAspects call() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (device == null) {
                    device = submitAndGetResult(teaseLibAI::getCaptureDevice);
                    if (device == null) {
                        Thread.sleep(TeaseLibAI.CAPTURE_DEVICE_POLL_DURATION_MILLIS);
                        continue;
                    } else {
                        device.start();
                        humanPose = getModel(Interest.Status);
                    }
                }
                estimatePoses();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                Exception cause = ExceptionUtil.reduce(e);
                logger.error(cause.getMessage(), cause);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
        if (humanPose != null) {
            submit(humanPose::close);
            humanPose = null;
        }
        if (device != null) {
            device.stop();
            device = null;
        }
        return PoseAspects.Unavailable;
    }

    private void estimatePoses() throws InterruptedException, ExecutionException {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                estimatePose();
            }
        } catch (SceneCapture.DeviceLost e) {
            HumanPoseDeviceInteraction.logger.warn(e.getMessage());
            device.stop();
            device = submit(() -> teaseLibAI.awaitCaptureDevice()).get();
            if (device == null) {
                Thread.sleep(10000);
            }
        }
    }

    public void setPause(Runnable task) {
        pause.set(task);
    }

    public void estimatePose() throws InterruptedException, ExecutionException {
        pause.updateAndGet(task -> {
            task.run();
            return Noop;
        });
        estimatePoses.lockInterruptibly();
        PoseAspects previous;
        Actor actor;
        Set<Interest> interests;
        try {
            previous = poseAspects.get();
            while ((actor = interaction.scriptRenderer.currentActor()) == null
                    || (interests = interestOf(actor)).isEmpty()) {
                PoseAspects unavailable = PoseAspects.Unavailable;
                if (poseAspects.get() != unavailable) {
                    poseAspects.set(unavailable);
                    signal(actor, unavailable, previous);
                }
                // TODO sleep until actor changes and interests not empty
                Thread.sleep(1000);
                // TODO Listen to actorChanged event and adding/removing listeners -> wait here for condition

            }
        } finally {
            estimatePoses.unlock();
        }

        estimatePose(actor, previous, interests);
    }

    private void estimatePose(Actor actor, PoseAspects previous, Set<Interest> interests)
            throws InterruptedException, ExecutionException {
        PoseAspects update = inferenceExecutor.submit(() -> getPoseAspects(interests, previous)).get();
        poseAspects.set(update);
        signal(actor, update, previous);
        if (update.is(HumanPose.Status.Stream)) {
            ensureFrametimeMillis(125);
        } else {
            ensureFrametimeMillis(500);
        }
    }

    private Set<Interest> interestOf(Actor actor) {
        return interaction.definitions(actor).keySet();
    }

    private void ensureFrametimeMillis(long frameTimeMillis) throws InterruptedException {
        long now = System.currentTimeMillis();
        long timestamp = humanPose.getTimestamp();
        long estimationMillis = now - timestamp;
        long durationMillis = Math.max(0, frameTimeMillis - estimationMillis);
        if (durationMillis > 0) {
            Thread.sleep(durationMillis);
        }
    }

    private PoseAspects getPoseAspects(Set<Interest> interests, PoseAspects previous) {
        // TODO Select the human pose model that matches the interests
        humanPose.setInterests(interests);
        List<HumanPose.Estimation> poses = humanPose.poses(device);
        if (poses.isEmpty()) {
            return PoseAspects.Unavailable;
        } else {
            return new PoseAspects(poses.get(0), interests, previous);
        }
    }

    private Set<Interest> signal(Actor actor, PoseAspects update, PoseAspects previous) {
        if (theSameActor(actor)) {
            // TODO fire only stream-relevant interests (Gaze), but not proximity
            // - stream relevant aspects are fired always, change relevant only when they've changed
            if (update.is(HumanPose.Status.Stream) || !update.equals(previous)) {
                DeviceInteractionDefinitions<Interest, EventSource<PoseEstimationEventArgs>> definitions = interaction
                        .definitions(actor);
                Set<Interest> previousInterests = definitions.keySet();

                PoseEstimationEventArgs eventArgs = new PoseEstimationEventArgs(actor, update,
                        humanPose.getTimestamp());
                definitions.stream().filter(e -> update.is(e.getKey())).map(Map.Entry::getValue)
                        .forEach(e -> e.fire(eventArgs));

                awaitPose.lock();
                try {
                    poseChanged.signalAll();
                } finally {
                    awaitPose.unlock();
                }

                Set<Interest> updated = new HashSet<>(update.interests);
                updated.removeAll(previousInterests);
                return updated;
            } else {
                return Collections.emptySet();
            }
        } else {
            return Collections.emptySet();
        }
    }

    private boolean theSameActor(Actor actor) {
        return actor == interaction.scriptRenderer.currentActor();
    }

}
