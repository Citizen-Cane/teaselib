package teaselib.core.ai.perception;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Closeable;
import teaselib.core.DeviceInteractionDefinitions;
import teaselib.core.ai.TeaseLibAI;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.PoseAspect;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.events.EventSource;

class PoseEstimationTask implements Callable<PoseAspects>, Closeable {
    private static final int PROXIMITY_SENSOR_FRAMERATE_MILLIS = 125;
    private static final int HEAD_GESTURE_FRAME_RATE_MILLIS = 125;

    static final Logger logger = LoggerFactory.getLogger(PoseEstimationTask.class);

    private final TeaseLibAI teaseLibAI;

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

    private DeviceInteractionDefinitions<Interest, EventSource<PoseEstimationEventArgs>> did = DeviceInteractionDefinitions.empty();

    private CountDownLatch startup = new CountDownLatch(1);

    PoseEstimationTask(TeaseLibAI teaseLibAI) throws InterruptedException {
        this.teaseLibAI = teaseLibAI;
        this.taskExecutor = NamedExecutorService.sameThread("Pose Estimation");
        this.inferenceExecutor = teaseLibAI.getExecutor(TeaseLibAI.ExecutionType.Accelerated);
        this.inferenceThread = inferenceExecutor.submitAndGet(Thread::currentThread);
        this.device = getDevice();
        this.future = taskExecutor.submit(this);
        startup.await();
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
                    if (passedMillis < PROXIMITY_SENSOR_FRAMERATE_MILLIS) {
                        return availablePose;
                    } else {
                        // TODO if pose task is running then
                        // await next pose completion instead of computing own pose with similar interests
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
        } catch (SceneCapture.DeviceLost e) {
            return PoseAspects.Unavailable;
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
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
        logger.info("Awaiting pose {}.{}", interests, aspects);
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

    private HumanPose loadModel() throws InterruptedException {
        this.humanPoseCachedModel = getModel(Interest.supported);
        return inferenceExecutor.submitAndGet(() -> {
            humanPoseCachedModel.loadModel(Interest.supported, device.rotation());
            return humanPoseCachedModel;
        });
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
        humanPoseCachedModel.setInterests(interests);
        return humanPoseCachedModel;
    }

    <T> T submitAndGet(Callable<T> task) throws InterruptedException {
        return inferenceExecutor.submitAndGet(task);
    }

    @Override
    public PoseAspects call() {
        try {
            estimatePoses.lockInterruptibly();
            startup.countDown();
            if (device != null) {
                loadModel();
            }

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (device == null) {
                        device = awaitCaptureDevice();
                    }
                    try {
                        estimatePoses.lockInterruptibly();
                        try {
                            estimatePoses();
                        } finally {
                            device.close();
                            estimatePoses.unlock();
                        }
                    } finally {
                        device = null;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    poseAspects.set(PoseAspects.Unavailable);
                }
                signalPoseChange();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            estimatePoses.unlock();
        }
        return PoseAspects.Unavailable;
    }

    private SceneCapture awaitCaptureDevice() throws InterruptedException {
        SceneCapture newDevice = null;
        while (newDevice == null) {
            newDevice = getDevice();
            if (newDevice == null) {
                poseChanged.await(SceneCapture.DEVICE_POLL_DURATION_MILLIS, TimeUnit.MILLISECONDS);
            }
        }
        return newDevice;
    }

    SceneCapture getDevice() throws InterruptedException {
        return inferenceExecutor.submitAndGet(SceneCapture::getDevice);
    }

    private void estimatePoses() throws InterruptedException {
        logger.info("Using capture device {}", device.name);
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Set<Interest> interests = awaitInterests();
                device.start();
                human.startTracking();
                while (!Thread.currentThread().isInterrupted()) {
                    estimatePose(poseAspects.get(), interests);
                    interests = awaitInterests();
                }
                device.stop();
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (SceneCapture.DeviceLost e) {
            logger.warn(e.getMessage());
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void setPause(Runnable task) {
        // TODO Cancel ongoing pose estimation when streaming
        pause.set(task);
    }

    public void clearPause() {
        pause.set(null);
    }

    public void setActor(DeviceInteractionDefinitions<Interest, EventSource<PoseEstimationEventArgs>> definitions) {
        try {
            estimatePoses.lockInterruptibly();
            try {
                did = definitions;
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
                awaitInterestChange();
                interests = gatherInterests();
            } while (interests.isEmpty());
            human.startTracking();
        }
        return interests;
    }

    private void awaitInterestChange() throws InterruptedException {
        if (poseAspects.get() != PoseAspects.Unavailable) {
            PoseAspects previous = poseAspects.get();
            poseAspects.set(PoseAspects.Unavailable);
            signal(PoseAspects.Unavailable, previous);
        }
        interestChange.await();
    }

    private Set<Interest> gatherInterests() {
        Set<Interest> poseInterests = awaitPoseInterests.get();
        if (did.actor == null) {
            return Collections.unmodifiableSet(poseInterests);
        } else {
            Set<Interest> actorInterests = did.keySet();
            return joinInterests(actorInterests, poseInterests);
        }
    }

    private void estimatePose(PoseAspects previous, Set<Interest> interests) throws InterruptedException {
        PoseAspects update = getLatestPoseAspects(previous, interests);
        poseAspects.set(update);

        // allow polling threads to get or await a pose
        // - avoids deadlock when polling thread and event have locked the same resource
        estimatePoses.unlock();
        try {
            signal(update, previous);
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

    private void signal(PoseAspects update, PoseAspects previous) {
        // TODO fire only stream-relevant interests (Gaze), but not proximity
        // - stream relevant aspects are fired always, fire relevant only when they've changed
        if (update.is(HumanPose.Status.Stream) || !update.equals(previous)) {
            var eventArgs = new PoseEstimationEventArgs(did.actor, update);
            fireEvent(eventArgs);
            signalPoseChange();
        }
    }

    private void fireEvent(PoseEstimationEventArgs eventArgs) {
        did.stream()
                .filter(entry -> eventArgs.pose.is(entry.getKey()))
                .map(Map.Entry::getValue)
                .forEach(event -> event.fire(eventArgs));
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
