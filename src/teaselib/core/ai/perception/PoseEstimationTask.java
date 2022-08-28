package teaselib.core.ai.perception;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
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

    // TODO new Person or clear when device changes
    private final Person human = new Person();

    private SceneCapture device = null;
    private HumanPose humanPoseCachedModel = null;
    private Actor currentActor;

    PoseEstimationTask(TeaseLibAI teaseLibAI, HumanPoseDeviceInteraction humanPoseDeviceInteraction)
            throws InterruptedException {
        this.teaseLibAI = teaseLibAI;
        this.interaction = humanPoseDeviceInteraction;

        this.taskExecutor = NamedExecutorService.sameThread("Pose Estimation");
        this.inferenceExecutor = teaseLibAI.getExecutor(TeaseLibAI.ExecutionType.Accelerated);
        this.inferenceThread = inferenceExecutor.submitAndGet(Thread::currentThread);
        inferenceExecutor.submitAndGet(() -> getModel(Interest.supported));
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

        human.close();
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
                    long passedMillis = System.currentTimeMillis() - availablePose.timestamp;
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

    // TODO remove multiple cached models since HumanPose handles interests already
    HumanPose getModel(Set<Interest> interests) throws InterruptedException {
        throwIfUnsupported(interests);
        // TODO cache models by interest or remove interest argument
        if (humanPoseCachedModel == null) {
            if (Thread.currentThread() == this.inferenceThread) {
                humanPoseCachedModel = teaseLibAI.getModel(interests);
            } else {
                humanPoseCachedModel = inferenceExecutor.submitAndGet(() -> teaseLibAI.getModel(interests));
            }
        }
        return humanPoseCachedModel;
    }

    <T> T submitAndGet(Callable<T> task) throws InterruptedException {
        return inferenceExecutor.submitAndGet(task);
    }

    @Override
    public PoseAspects call() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                device = awaitCaptureDevice();
                estimatePoses.lockInterruptibly();
                try {
                    estimatePoses();
                } finally {
                    estimatePoses.unlock();
                    device.close();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                device = null;
            }
            poseAspects.set(PoseAspects.Unavailable);
            signalPoseChange();
        }
        return PoseAspects.Unavailable;
    }

    private void estimatePoses() throws InterruptedException {
        logger.info("Using capture device {}", device.name);
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Set<Interest> interests = awaitInterests();
                device.start();
                human.startTracking();
                while (!Thread.currentThread().isInterrupted()) {
                    estimatePose(currentActor, poseAspects.get(), interests);
                    interests = awaitInterests();
                }
                device.stop();
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (SceneCapture.DeviceLost e) {
            HumanPoseDeviceInteraction.logger.warn(e.getMessage());
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
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
        }
    }

    public void interestChanged() {
        try {
            estimatePoses.lockInterruptibly();
            try {
                interestChange.signalAll();
            } finally {
                estimatePoses.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Set<Interest> awaitInterests() throws InterruptedException {
        Set<Interest> interests = gatherInterests();
        if (interests.isEmpty()) {
            do {
                awaitInterestChange(currentActor);
                interests = gatherInterests();
            } while (interests.isEmpty());
            human.startTracking();
        }
        return interests;
    }

    private Set<Interest> gatherInterests() {
        Set<Interest> poseInterests = awaitPoseInterests.get();
        Set<Interest> actorInterests = currentActor == null ? Collections.emptySet()
                : interaction.definitions(currentActor).keySet();
        return joinInterests(actorInterests, poseInterests);
    }

    private void awaitInterestChange(Actor actor) throws InterruptedException {
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

        // allow polling threads to get or await a pose
        // - avoids deadlock when polling thread and event have locked the same resource
        estimatePoses.unlock();
        try {
            signal(actor, update, previous);
        } finally {
            estimatePoses.lockInterruptibly();
        }

        long frameTimeMillis = update.is(HumanPose.Status.Stream) //
                ? HEAD_GESTURE_FRAME_RATE_MILLIS
                : PROXIMITY_SENSOR_FRAMERATE_MILLIS;
        interestChange.awaitUntil(new Date(update.timestamp + frameTimeMillis));
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

    private static Set<Interest> joinInterests(Set<Interest> actorInterests, Set<Interest> more) {
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

    private PoseAspects getPoseAspects(Set<Interest> interests) {
        return getPoseAspects(PoseAspects.Unavailable, interests);
    }

    private PoseAspects getPoseAspects(PoseAspects previous, Set<Interest> interests) {
        wakeUpFromHibernate(device);
        try {
            HumanPose model = getModel(interests);
            model.setInterests(interests);
            long timestamp = System.currentTimeMillis();
            human.update(model, device, device.rotation().reverse().value, timestamp);
            var pose = human.pose();
            if (pose == HumanPose.Estimation.NONE) {
                return PoseAspects.Unavailable;
            } else {
                return new PoseAspects(pose, timestamp, interests, previous);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PoseAspects.Unavailable;
        }
    }

    private static void wakeUpFromHibernate(SceneCapture device) {
        if (!device.isStarted()) {
            device.start();
        }
    }

    private void signal(Actor actor, PoseAspects update, PoseAspects previous) {
        // TODO fire only stream-relevant interests (Gaze), but not proximity
        // - stream relevant aspects are fired always, change relevant only when they've changed
        if (update.is(HumanPose.Status.Stream) || !update.equals(previous)) {
            var definitions = interaction.definitions(actor);
            var eventArgs = new PoseEstimationEventArgs(actor, update);
            definitions.stream().filter(e -> update.is(e.getKey())).map(Map.Entry::getValue)
                    .forEach(e -> e.fire(eventArgs));
            signalPoseChange();
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
