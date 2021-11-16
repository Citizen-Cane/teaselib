package teaselib.core.ai.perception;

import static teaselib.core.util.ExceptionUtil.asRuntimeException;
import static teaselib.core.util.ExceptionUtil.reduce;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
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
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.core.Closeable;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.ai.TeaseLibAI;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.PoseAspect;
import teaselib.core.concurrency.NamedExecutorService;

class PoseEstimationTask implements Callable<PoseAspects>, Closeable {
    private static final int PROXIMITY_SENSOR_FRAMERATE_MILLIS = 500;
    private static final int HEAD_GESTURE_FRAME_RATE_MILLIS = 125;

    static final Logger logger = LoggerFactory.getLogger(PoseEstimationTask.class);

    private static final Runnable Noop = () -> { //
    };

    private final TeaseLibAI teaseLibAI;
    private final HumanPoseDeviceInteraction interaction;

    private final NamedExecutorService taskExecutor;
    private Future<PoseAspects> future = null;

    private final ExecutorService inferenceExecutor;
    private final Thread inferenceThread;
    private final Lock awaitPose = new ReentrantLock();
    private final AtomicReference<Set<Interest>> awaitInterests = new AtomicReference<>(Collections.emptySet());
    private final Condition poseChanged = awaitPose.newCondition();
    private final AtomicReference<PoseAspects> poseAspects = new AtomicReference<>(PoseAspects.Unavailable);
    private final ReentrantLock estimatePoses = new ReentrantLock();
    private final AtomicReference<Runnable> pause = new AtomicReference<>(Noop);

    private HumanPose humanPose = null;

    PoseEstimationTask(TeaseLibAI teaseLibAI, HumanPoseDeviceInteraction humanPoseDeviceInteraction) {
        this.teaseLibAI = teaseLibAI;

        this.interaction = humanPoseDeviceInteraction;
        this.taskExecutor = NamedExecutorService.sameThread("Pose Estimation");

        this.inferenceExecutor = teaseLibAI.getExecutor(TeaseLibAI.ExecutionType.Accelerated);
        this.inferenceThread = submitAndGetResult(Thread::currentThread);

        this.future = taskExecutor.submit(this);
    }

    @Override
    public void close() {
        taskExecutor.shutdown();

        if (future != null && !future.isDone() && !future.isCancelled()) {
            future.cancel(true);
        }

        try {
            taskExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public PoseAspects getPose(Set<Interest> interests) {
        throwIfUnsupported(interests);
        // TODO check whether the current model/poseAspects matches the requested interests
        return poseAspects.get();
        // TODO signal when waiting and scene capture device lost - exception or special pose, probably PoseAspects.None
    }

    private static void throwIfUnsupported(Set<Interest> interests) {
        Objects.requireNonNull(interests);

        if (interests.stream().anyMatch(Predicate.not(Interest.supported::contains))) {
            throw new NoSuchElementException("No pose estimation model available to match " + interests);
        }
    }

    // TODO replace the aspects with a condition a l Requires.all(...), Requires.any(...)
    boolean awaitPose(Set<Interest> interests, long duration, TimeUnit unit, PoseAspect... aspects) {
        throwIfUnsupported(interests);
        var pose = poseAspects.get();
        if (pose.containsAll(interests) && pose.is(aspects)) {
            return true;
        } else {
            try {
                awaitPose.lockInterruptibly();
                awaitInterests.set(interests);
                try {
                    logger.info("Awaiting pose {} and {}", interests, aspects);
                    while (poseChanged.await(duration, unit)) {
                        pose = poseAspects.get();
                        if (pose.is(aspects)) {
                            return true;
                        }
                    }
                    return false;
                } finally {
                    awaitInterests.set(Collections.emptySet());
                    awaitPose.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ScriptInterruptedException();
            }
        }
        // TODO signal when waiting and scene capture device lost - exception or special pose, probably PoseAspects.None
    }

    HumanPose getModel(Set<Interest> interests) {
        throwIfUnsupported(interests);
        if (humanPose == null) {
            if (Thread.currentThread() == this.inferenceThread) {
                humanPose = teaseLibAI.getModel(interests);
            } else {
                humanPose = submitAndGetResult(() -> teaseLibAI.getModel(interests));
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

    private <T> Future<T> submit(Callable<T> task) {
        return inferenceExecutor.submit(task::call);
    }

    @Override
    public PoseAspects call() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                try (SceneCapture device = awaitCaptureDevice()) {
                    estimatePoses(device);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return PoseAspects.Unavailable;
    }

    private void estimatePoses(SceneCapture device) {
        logger.info("Using capture device {}", device.name);
        try {
            device.start();
            // TODO select model according to interests of active listeners
            humanPose = getModel(Interest.supported);
            while (!Thread.currentThread().isInterrupted()) {
                estimatePose(device);
            }
        } catch (SceneCapture.DeviceLost e) {
            HumanPoseDeviceInteraction.logger.warn(e.getMessage());
            device.stop();
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        } finally {
            humanPose = null;
        }
    }

    public boolean isActive() {
        return humanPose != null;
    }

    private SceneCapture awaitCaptureDevice() throws InterruptedException {
        SceneCapture newDevice = null;
        while (newDevice == null) {
            newDevice = submitAndGetResult(SceneCapture::getDevice);
            if (newDevice == null) {
                Thread.sleep(SceneCapture.DEVICE_POLL_DURATION_MILLIS);
            }
        }
        return newDevice;
    }

    public void setPause(Runnable task) {
        pause.set(task);
    }

    public void estimatePose(SceneCapture device) throws InterruptedException {
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
                    || (interests = joinInterests(actor, awaitInterests.get())).isEmpty()) {
                PoseAspects unavailable = PoseAspects.Unavailable;
                if (poseAspects.get() != unavailable) {
                    poseAspects.set(unavailable);
                    signal(actor, unavailable, previous);
                }
                // TODO sleep until actor changes and interests not empty
                ensureFrametimeMillis(1000);
                // TODO Listen to actorChanged event and adding/removing listeners -> wait here for condition
            }
        } finally {
            estimatePoses.unlock();
        }

        estimatePose(actor, previous, interests, device);
    }

    private void estimatePose(Actor actor, PoseAspects previous, Set<Interest> interests, SceneCapture device)
            throws InterruptedException {
        PoseAspects update = submitAndGetResult(() -> getPoseAspects(previous, interests, device));
        poseAspects.set(update);
        signal(actor, update, previous);
        if (update.is(HumanPose.Status.Stream)) {
            ensureFrametimeMillis(HEAD_GESTURE_FRAME_RATE_MILLIS);
        } else {
            ensureFrametimeMillis(PROXIMITY_SENSOR_FRAMERATE_MILLIS);
        }
    }

    private Set<Interest> joinInterests(Actor actor, Set<Interest> more) {
        Set<Interest> actorInterests = interaction.definitions(actor).keySet();
        if (actorInterests.isEmpty()) {
            return more;
        } else if (more.isEmpty()) {
            return actorInterests;
        } else {
            Set<Interest> joined = new HashSet<>(actorInterests.size() + more.size());
            joined.addAll(actorInterests);
            joined.addAll(more);
            return joined;
        }
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

    private PoseAspects getPoseAspects(PoseAspects previous, Set<Interest> interests, SceneCapture device) {
        // TODO Select the human pose model that matches the interests
        humanPose.setInterests(interests);
        wakeUpFromHibernate(device);
        List<HumanPose.Estimation> poses = humanPose.poses(device);
        if (poses.isEmpty()) {
            return PoseAspects.Unavailable;
        } else {
            return new PoseAspects(poses.get(0), interests, previous);
        }
    }

    private static void wakeUpFromHibernate(SceneCapture device) {
        if (!device.isStarted()) {
            device.start();
        }
    }

    private Set<Interest> signal(Actor actor, PoseAspects update, PoseAspects previous) {
        if (theSameActor(actor)) {
            // TODO fire only stream-relevant interests (Gaze), but not proximity
            // - stream relevant aspects are fired always, change relevant only when they've changed
            if (update.is(HumanPose.Status.Stream) || !update.equals(previous)) {
                var definitions = interaction.definitions(actor);
                Set<Interest> previousInterests = definitions.keySet();

                var eventArgs = new PoseEstimationEventArgs(actor, update, humanPose.getTimestamp());
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
