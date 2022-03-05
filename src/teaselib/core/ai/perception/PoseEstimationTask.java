package teaselib.core.ai.perception;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
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
import teaselib.core.ai.TeaseLibAI;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.PoseAspect;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.concurrency.NamedExecutorService;

class PoseEstimationTask implements Callable<PoseAspects>, Closeable {
    private static final int PROXIMITY_SENSOR_FRAMERATE_MILLIS = 500;
    private static final int HEAD_GESTURE_FRAME_RATE_MILLIS = 125;

    static final Logger logger = LoggerFactory.getLogger(PoseEstimationTask.class);

    private final TeaseLibAI teaseLibAI;
    private final HumanPoseDeviceInteraction interaction;

    private final NamedExecutorService taskExecutor;
    private Future<PoseAspects> future = null;

    private final NamedExecutorService inferenceExecutor;
    private final Thread inferenceThread;
    private final Lock awaitPose = new ReentrantLock();
    private final AtomicReference<Set<Interest>> awaitPoseInterests = new AtomicReference<>(Collections.emptySet());
    private final Condition poseChanged = awaitPose.newCondition();
    private final AtomicReference<PoseAspects> poseAspects = new AtomicReference<>(PoseAspects.Unavailable);
    private final ReentrantLock estimatePoses = new ReentrantLock();
    private final Condition interestChange = estimatePoses.newCondition();
    private final AtomicReference<Runnable> pause = new AtomicReference<>(null);

    private SceneCapture device = null;
    private HumanPose humanPose = null;
    private Actor currentActor;

    PoseEstimationTask(TeaseLibAI teaseLibAI, HumanPoseDeviceInteraction humanPoseDeviceInteraction)
            throws InterruptedException {
        this.teaseLibAI = teaseLibAI;
        this.interaction = humanPoseDeviceInteraction;

        this.taskExecutor = NamedExecutorService.sameThread("Pose Estimation");
        this.inferenceExecutor = teaseLibAI.getExecutor(TeaseLibAI.ExecutionType.Accelerated);
        this.inferenceThread = inferenceExecutor.submitAndGet(Thread::currentThread);

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

    public boolean isActive() {
        return device != null;
    }

    public PoseAspects getPose(Set<Interest> interests) {
        throwIfUnsupported(interests);
        try {
            estimatePoses.lockInterruptibly();
            try {
                var availablePose = poseAspects.get();
                if (availablePose.containsAll(interests)) {
                    long passedMillis = System.currentTimeMillis() - humanPose.getTimestamp();
                    if (passedMillis < 250) {
                        return availablePose;
                    } else {
                        return computePose(interests);
                    }
                } else {
                    return computePose(interests);
                }
            } finally {
                estimatePoses.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PoseAspects.Unavailable;
        }
    }

    private PoseAspects computePose(Set<Interest> interests) throws InterruptedException {
        return inferenceExecutor.submitAndGet(() -> getPoseAspects(interests));
    }

    private static void throwIfUnsupported(Set<Interest> interests) {
        Objects.requireNonNull(interests);

        if (interests.stream().anyMatch(Predicate.not(Interest.supported::contains))) {
            throw new NoSuchElementException("No pose estimation model available to match " + interests);
        }
    }

    // TODO replace the aspects with a condition like Requires.all(...), Requires.any(...)
    // - currently awaitPose implements "any aspect" because PoseAspects.is(PoseAspect) implements "Any"
    boolean awaitPose(Set<Interest> interests, long duration, TimeUnit unit, PoseAspect... aspects)
            throws InterruptedException {
        throwIfUnsupported(interests);
        estimatePoses.lockInterruptibly();
        try {
            awaitPose.lockInterruptibly();
            try {
                return getCurrentOrAwaitPose(interests, duration, unit, aspects);
            } finally {
                awaitPose.unlock();
            }
        } finally {
            if (estimatePoses.isHeldByCurrentThread()) {
                estimatePoses.unlock();
            }
        }
    }

    private boolean getCurrentOrAwaitPose(Set<Interest> interests, long duration, TimeUnit unit, PoseAspect... aspects)
            throws InterruptedException {
        PoseAspects availablePose = poseAspects.get();
        if (availablePose.containsAll(interests) && availablePose.is(aspects)) {
            return true;
        } else {
            awaitPoseInterests.set(interests);
            try {
                interestChange.signalAll();
                estimatePoses.unlock();
                return awaitFuturePose(interests, duration, unit, aspects);
            } finally {
                awaitPoseInterests.set(Collections.emptySet());
            }
        }
    }

    private boolean awaitFuturePose(Set<Interest> interests, long duration, TimeUnit unit, PoseAspect... aspects)
            throws InterruptedException {
        logger.info("Awaiting pose {} and {}", interests, aspects);
        var now = System.currentTimeMillis();
        long durationMillis = unit.toMillis(duration);
        var deadline = new Date(durationMillis < Long.MAX_VALUE - now ? durationMillis + now : Long.MAX_VALUE);
        while (poseChanged.awaitUntil(deadline)) {
            var availablePose = poseAspects.get();
            if (availablePose.is(aspects)) {
                return true;
            } else if (availablePose.equals(PoseAspects.Unavailable)) {
                if (interests.contains(Interest.Proximity) && HumanPose.asSet(aspects).contains(Proximity.AWAY)) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    HumanPose getModel(Set<Interest> interests) throws InterruptedException {
        throwIfUnsupported(interests);
        if (humanPose == null) {
            if (Thread.currentThread() == this.inferenceThread) {
                humanPose = teaseLibAI.getModel(interests);
            } else {
                humanPose = inferenceExecutor.submitAndGet(() -> teaseLibAI.getModel(interests));
            }
        }
        return humanPose;
    }

    <T> T submitAndGet(Callable<T> task) throws InterruptedException {
        return inferenceExecutor.submitAndGet(task);
    }

    @Override
    public PoseAspects call() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                device = awaitCaptureDevice();
                try {
                    estimatePoses();
                } finally {
                    device.close();
                    device = null;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            poseAspects.set(PoseAspects.Unavailable);
            signalPoseChange();
        }
        return PoseAspects.Unavailable;
    }

    private void estimatePoses() throws InterruptedException {
        logger.info("Using capture device {}", device.name);
        try {
            device.start();
            // TODO select model according to interests of active listeners
            humanPose = getModel(Interest.supported);
            while (!Thread.currentThread().isInterrupted()) {
                estimatePose();
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (SceneCapture.DeviceLost e) {
            HumanPoseDeviceInteraction.logger.warn(e.getMessage());
            device.stop();
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        } finally {
            humanPose = null;
        }
    }

    private SceneCapture awaitCaptureDevice() throws InterruptedException {
        SceneCapture newDevice = null;
        while (newDevice == null) {
            newDevice = inferenceExecutor.submitAndGet(SceneCapture::getDevice);
            if (newDevice == null) {
                Thread.sleep(SceneCapture.DEVICE_POLL_DURATION_MILLIS);
            }
        }
        return newDevice;
    }

    public void setPause(Runnable task) {
        // TODO Cancel ongoing pose estimation when streaming
        pause.set(task);
    }

    public void clearPause() {
        pause.set(null);
    }

    public void setActor(Actor actor) {
        try {
            estimatePoses.lockInterruptibly();
            try {
                currentActor = actor;
                interestChange.signalAll();
            } finally {
                estimatePoses.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ;
        }
    }

    public void estimatePose() throws InterruptedException {
        estimatePoses.lockInterruptibly();
        Set<Interest> interests;
        try {
            while (true) {

                Set<Interest> poseInterests = awaitPoseInterests.get();
                Set<Interest> actorInterests = currentActor == null ? Collections.emptySet()
                        : interaction.definitions(currentActor).keySet();
                interests = joinInterests(actorInterests, poseInterests);
                if (interests.isEmpty()) {
                    awaitInterests(currentActor);
                } else {
                    break;
                }
            }
            estimatePose(currentActor, poseAspects.get(), interests);
        } finally {
            estimatePoses.unlock();
        }
    }

    private void awaitInterests(Actor actor) throws InterruptedException {
        if (poseAspects.get() != PoseAspects.Unavailable) {
            PoseAspects previous = poseAspects.get();
            poseAspects.set(PoseAspects.Unavailable);
            signal(actor, PoseAspects.Unavailable, previous);
        }
        interestChange.await();
    }

    private void estimatePose(Actor actor, PoseAspects previous, Set<Interest> interests) throws InterruptedException {
        PoseAspects update = getLatestPoseAspects(previous, interests);
        poseAspects.set(update);
        signal(actor, update, previous);
        if (update.is(HumanPose.Status.Stream)) {
            ensureFrametimeMillis(HEAD_GESTURE_FRAME_RATE_MILLIS);
        } else {
            ensureFrametimeMillis(PROXIMITY_SENSOR_FRAMERATE_MILLIS);
        }
    }

    private PoseAspects getLatestPoseAspects(PoseAspects previous, Set<Interest> interests)
            throws InterruptedException {
        PoseAspects update = null;
        do {
            Runnable task;
            while ((task = pause.getAndSet(null)) != null) {
                task.run();
            }
            update = inferenceExecutor.submitAndGet(() -> getPoseAspects(previous, interests));
            while ((task = pause.getAndSet(null)) != null) {
                task.run();
                update = null;
            }
        } while (update == null);
        return update;
    }

    private Set<Interest> joinInterests(Set<Interest> actorInterests, Set<Interest> more) {
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
            interestChange.await(durationMillis, TimeUnit.MILLISECONDS);
        }
    }

    private PoseAspects getPoseAspects(PoseAspects previous, Set<Interest> interests) {
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

    private PoseAspects getPoseAspects(Set<Interest> interests) {
        // TODO Select the human pose model that matches the interests
        humanPose.setInterests(interests);
        wakeUpFromHibernate(device);
        List<HumanPose.Estimation> poses = humanPose.poses(device);
        if (poses.isEmpty()) {
            return PoseAspects.Unavailable;
        } else {
            return new PoseAspects(poses.get(0), interests);
        }
    }

    private static void wakeUpFromHibernate(SceneCapture device) {
        if (!device.isStarted()) {
            device.start();
        }
    }

    private void signal(Actor actor, PoseAspects update, PoseAspects previous) {
        if (actor == currentActor) {
            // TODO fire only stream-relevant interests (Gaze), but not proximity
            // - stream relevant aspects are fired always, change relevant only when they've changed
            if (update.is(HumanPose.Status.Stream) || !update.equals(previous)) {
                var definitions = interaction.definitions(actor);
                var eventArgs = new PoseEstimationEventArgs(actor, update, humanPose.getTimestamp());
                definitions.stream().filter(e -> update.is(e.getKey())).map(Map.Entry::getValue)
                        .forEach(e -> e.fire(eventArgs));
                signalPoseChange();
            }
        }
    }

    private void signalPoseChange() {
        awaitPose.lock();
        try {
            poseChanged.signalAll();
        } finally {
            awaitPose.unlock();
        }
    }

}
