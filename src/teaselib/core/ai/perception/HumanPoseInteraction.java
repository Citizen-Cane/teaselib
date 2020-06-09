package teaselib.core.ai.perception;

import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Map;
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
        // TODO set flag to stop if aspects are a subset of desired aspects.
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
        private final HumanPose humanPose;

        HumanPoseInteraction interactions;

        private final UnaryOperator<Float> awarenessHysteresis = Hysteresis.function(0.0f, 1.0f, 0.25f);

        PoseEstimationTask(HumanPoseInteraction interactions) {
            this.interactions = interactions;
            // TODO init all in worker task
            this.teaseLibAI = new TeaseLibAI();
            List<SceneCapture> sceneCaptures = teaseLibAI.sceneCaptures();
            this.humanPose = new HumanPose(sceneCaptures.get(0));
        }

        @Override
        public Pose call() throws Exception {
            Pose pose = new Pose(Aspect.None);
            try {
                while (!Thread.interrupted()) {
                    Actor actor = interactions.scriptRenderer.currentActor();
                    ScriptInteractionImplementation<Aspect, Reaction>.Definitions reactions = interactions.defs(actor);
                    Set<Aspect> aspects = reactions.stream().map(Map.Entry<Aspect, Reaction>::getValue)
                            .map(e -> e.aspect).collect(toSet());

                    if (!aspects.isEmpty()) {
                        humanPose.setDesiredAspects(aspects);
                        int n = humanPose.estimate();
                        if (actor == interactions.scriptRenderer.currentActor()) {
                            if (aspects.contains(Aspect.Awareness)) {

                                float y = awarenessHysteresis.apply(n >= 1 ? 1.0f : 0.0f);
                                pose = new Pose(y >= 0.5f ? Aspect.Awareness : Aspect.None);
                                // TODO define max-reliable distance for each model to avoid dropouts in the distance
                                Pose current = interactions.current;
                                if (!pose.equals(current)) {
                                    reactions.get(Aspect.Awareness).consumer.accept(pose);
                                    interactions.current = pose;
                                }
                            } else {
                                synchronized (this) {
                                    wait(1000);
                                }
                            }
                        }
                    }

                    synchronized (this) {
                        wait(500);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
                throw e;
            }

            return pose;
        }

    }

}
