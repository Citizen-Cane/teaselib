package teaselib.core.ai.perception;

import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.core.Closeable;
import teaselib.core.ScriptInteractionImplementation;
import teaselib.core.ScriptRenderer;
import teaselib.core.ai.TeaseLibAI;
import teaselib.core.ai.perception.HumanPose.Aspect;
import teaselib.core.ai.perception.HumanPoseInteraction.Reaction;
import teaselib.core.ai.perception.SceneCapture.EnclosureLocation;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.util.math.Hysteresis;

public class HumanPoseInteraction extends ScriptInteractionImplementation<HumanPose.Aspect, Reaction>
        implements Closeable {
    static final Logger logger = LoggerFactory.getLogger(HumanPoseInteraction.class);

    private final NamedExecutorService poseEstimation;
    private final PoseEstimationTask task;
    private Future<Pose> future = null;
    final ScriptRenderer scriptRenderer;

    Pose current = new Pose(Aspect.None);

    public static class Reaction {
        final Aspect aspect;
        final Consumer<Pose> consumer;

        public Reaction(Aspect aspect, Consumer<Pose> consumer) {
            this.aspect = aspect;
            this.consumer = consumer;
        }
    }

    public static class Pose {
        public static final Pose NONE = new Pose(Aspect.None);

        public final Aspect aspect;

        Pose(Aspect aspect) {
            this.aspect = aspect;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((aspect == null) ? 0 : aspect.hashCode());
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
            if (aspect != other.aspect)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return aspect.toString();
        }

    }

    @Override
    public void close() {
        future.cancel(true);
        poseEstimation.shutdown();
    }

    public HumanPoseInteraction(ScriptRenderer scriptRenderer) {
        super(Object::equals);
        this.scriptRenderer = scriptRenderer;

        poseEstimation = NamedExecutorService.sameThread("Pose Estimation");
        task = new PoseEstimationTask(this);
        future = poseEstimation.submit(task);
        // TODO crashes, no error reporting or main thread forwarding
    }

    public Pose getCurrentPose(Aspect aspect) {
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

        return current;
    }

    ScriptInteractionImplementation<Aspect, Reaction>.Definitions defs(Actor actor) {
        return super.definitions(actor);
    }

    static class PoseEstimationTask implements Callable<Pose> {
        private final TeaseLibAI teaseLibAI;

        HumanPoseInteraction interactions;

        private final UnaryOperator<Float> awarenessHysteresis = Hysteresis.function(0.0f, 1.0f, 0.25f);

        PoseEstimationTask(HumanPoseInteraction interactions) {
            this.interactions = interactions;
            this.teaseLibAI = new TeaseLibAI();
        }

        @Override
        public Pose call() throws InterruptedException {
            SceneCapture device = awaitCaptureDevice();
            try (HumanPose humanPose = new HumanPose(device);) {
                try {
                    return estimate(humanPose);
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                    throw e;
                }
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

        private Pose estimate(HumanPose humanPose) throws InterruptedException {
            Pose pose = Pose.NONE;
            while (!Thread.interrupted()) {
                Actor actor = interactions.scriptRenderer.currentActor();
                pose = get(actor, humanPose);
                signal(actor, pose);

                synchronized (this) {
                    wait(2000);
                }
            }
            return pose;
        }

        private Pose get(Actor actor, HumanPose humanPose) {
            ScriptInteractionImplementation<Aspect, Reaction>.Definitions reactions = interactions.defs(actor);
            Set<Aspect> aspects = reactions.stream().map(Map.Entry<Aspect, Reaction>::getValue).map(e -> e.aspect)
                    .collect(toSet());

            if (aspects.isEmpty()) {
                return Pose.NONE;
            } else {
                humanPose.setDesiredAspects(aspects);
                int n = humanPose.estimate();
                if (aspects.contains(Aspect.Awareness)) {
                    float y = awarenessHysteresis.apply(n >= 1 ? 1.0f : 0.0f);
                    return new Pose(y >= 0.5f ? Aspect.Awareness : Aspect.None);
                    // TODO define max-reliable distance for each model to avoid drop-outs in the
                    // distance
                    // TODO measure distance by head/shoulder/hip width
                } else {
                    return Pose.NONE;
                }
            }
        }

        private void signal(Actor actor, Pose pose) {
            if (theSameActor(actor)) {
                Pose current = interactions.current;
                if (!pose.equals(current)) {
                    ScriptInteractionImplementation<Aspect, Reaction>.Definitions reactions = interactions.defs(actor);
                    reactions.get(Aspect.Awareness).consumer.accept(pose);
                    interactions.current = pose;
                }
            }
        }

        private boolean theSameActor(Actor actor) {
            return actor == interactions.scriptRenderer.currentActor();
        }

    }

}
