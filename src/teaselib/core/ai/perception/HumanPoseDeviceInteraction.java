package teaselib.core.ai.perception;

import static java.util.stream.Collectors.toSet;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.core.Closeable;
import teaselib.core.DeviceInteractionImplementation;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.ScriptRenderer;
import teaselib.core.ai.TeaseLibAI;
import teaselib.core.ai.perception.HumanPose.Interests;
import teaselib.core.ai.perception.HumanPose.PoseAspect;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.ai.perception.HumanPose.Status;
import teaselib.core.ai.perception.HumanPoseDeviceInteraction.Reaction;
import teaselib.core.ai.perception.SceneCapture.EnclosureLocation;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.util.math.Hysteresis;

public class HumanPoseDeviceInteraction extends DeviceInteractionImplementation<HumanPose.Interests, Reaction>
        implements Closeable {
    static final Logger logger = LoggerFactory.getLogger(HumanPoseDeviceInteraction.class);

    private final NamedExecutorService poseEstimation;
    private final PoseEstimationTask task;
    private Future<Pose> future = null;
    final ScriptRenderer scriptRenderer;

    public static class Reaction {
        final Interests aspect;
        final Consumer<Pose> consumer;

        public Reaction(Interests aspect, Consumer<Pose> consumer) {
            this.aspect = aspect;
            this.consumer = consumer;
        }
    }

    public static class Pose {
        public static final Pose Unavailable = new Pose(Collections.emptySet(), Status.None);

        private final Set<Interests> interests;
        private final Set<PoseAspect> aspects;

        Pose(Set<Interests> interests, PoseAspect... aspects) {
            this.interests = interests;
            this.aspects = Set.of(aspects);
        }

        public boolean is(PoseAspect... values) {
            for (PoseAspect value : values) {
                if (aspects.contains(value)) {
                    return true;
                }
            }
            return false;
        }

        public boolean containsAll(Interests... values) {
            for (Interests value : values) {
                if (!interests.contains(value)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((aspects == null) ? 0 : aspects.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Pose other = (Pose) obj;
            if (aspects == null) {
                if (other.aspects != null)
                    return false;
            } else if (!aspects.equals(other.aspects))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return aspects.toString();
        }

    }

    public HumanPoseDeviceInteraction(ScriptRenderer scriptRenderer) {
        super(Object::equals);
        this.scriptRenderer = scriptRenderer;

        poseEstimation = NamedExecutorService.sameThread("Pose Estimation");
        task = new PoseEstimationTask(this);
        future = poseEstimation.submit(task);
    }

    @Override
    public void close() {
        future.cancel(true);
        poseEstimation.shutdown();
    }

    public Pose getPose(Interests interests) {
        return task.getPose(interests);
    }

    public boolean awaitPose(Interests interests, long duration, TimeUnit unit, PoseAspect... aspects) {
        return task.awaitPose(interests, duration, unit, aspects);
    }

    DeviceInteractionImplementation<Interests, Reaction>.Definitions defs(Actor actor) {
        return super.definitions(actor);
    }

    static class PoseEstimationTask implements Callable<Pose> {
        private final TeaseLibAI teaseLibAI;

        HumanPoseDeviceInteraction interactions;

        Lock awaitPose = new ReentrantLock();
        Condition poseChanged = awaitPose.newCondition();
        AtomicReference<Pose> pose = new AtomicReference<>(Pose.Unavailable);

        private final UnaryOperator<Float> awarenessHysteresis = Hysteresis.function(0.0f, 1.0f, 0.25f);

        PoseEstimationTask(HumanPoseDeviceInteraction interactions) {
            this.interactions = interactions;
            this.teaseLibAI = new TeaseLibAI();
        }

        public Pose getPose(Interests interests) {
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

            return pose.get();
        }

        // TODO replace the apsects with a condition a lä Requires.all(...), Requires.any(...)
        public boolean awaitPose(Interests interests, long duration, TimeUnit unit, PoseAspect... aspects) {
            Pose result = pose.get();

            if (result.containsAll(interests) && result.is(aspects)) {
                return true;
            }

            // TODO set interests, use current is match, or schedule pose estimation that matches temporary interests
            // - works for now as long as we just detect human presence as a single interest

            try {
                awaitPose.lockInterruptibly();
                try {
                    while (poseChanged.await(duration, unit)) {
                        result = pose.get();
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
        public Pose call() throws InterruptedException {
            try {
                while (true) {
                    SceneCapture device = awaitCaptureDevice();
                    try (HumanPose humanPose = new HumanPose(device);) {
                        estimate(humanPose);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    } finally {
                        device.stop();
                    }
                }
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
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

        private void estimate(HumanPose humanPose) throws InterruptedException {
            while (!Thread.interrupted()) {
                Actor actor = interactions.scriptRenderer.currentActor();
                pose.updateAndGet(previous -> {
                    Pose update = get(actor, humanPose);
                    signal(actor, update, previous);
                    return update;
                });
                synchronized (this) {
                    wait(1000);
                }
            }
        }

        private Pose get(Actor actor, HumanPose humanPose) {
            // TODO Each interest can have only one handler - cannot just disable listeners (must unregister instead)
            DeviceInteractionImplementation<Interests, Reaction>.Definitions reactions = interactions.defs(actor);
            Set<Interests> interests = reactions.stream().map(Map.Entry<Interests, Reaction>::getValue)
                    .map(e -> e.aspect).collect(toSet());

            if (interests.isEmpty()) {
                return Pose.Unavailable;
            } else {
                humanPose.setInterests(interests);
                int n = humanPose.estimate();
                if (interests.contains(Interests.Proximity)) {
                    float y = awarenessHysteresis.apply(n >= 1 ? 1.0f : 0.0f);
                    PoseAspect[] face2face = { Status.Available, Proximity.FaceToFace, Proximity.Close,
                            Proximity.NotAway, Proximity.NotFar, Proximity.NotNear };
                    PoseAspect[] far = { Status.Available, Proximity.NotFaceToFace, Proximity.NotClose, Proximity.Away,
                            Proximity.Far, Proximity.Near };
                    return new Pose(interests, y >= 0.5f ? face2face : far);
                    // TODO define max-reliable distance for each model to avoid drop-outs in the
                    // distance
                    // TODO measure distance by head/shoulder/hip width
                } else {
                    return Pose.Unavailable;
                }
            }
        }

        private void signal(Actor actor, Pose update, Pose previous) {
            if (theSameActor(actor)) {
                if (!update.equals(previous)) {
                    // TODO When set differs, signal only the changed interests, not all
                    DeviceInteractionImplementation<Interests, Reaction>.Definitions reactions = interactions
                            .defs(actor);
                    reactions.get(Interests.Proximity).consumer.accept(update);

                    awaitPose.lock();
                    try {
                        poseChanged.signalAll();
                    } finally {
                        awaitPose.unlock();
                    }
                }
            }
        }

        private boolean theSameActor(Actor actor) {
            return actor == interactions.scriptRenderer.currentActor();
        }

    }

}
