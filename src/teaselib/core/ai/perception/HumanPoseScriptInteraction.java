package teaselib.core.ai.perception;

import static teaselib.core.ai.perception.HumanPose.Interest.Proximity;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import teaselib.Answer;
import teaselib.ScriptFunction;
import teaselib.core.DeviceInteractionImplementations;
import teaselib.core.Script;
import teaselib.core.ScriptInteraction;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.ai.perception.HumanPose.Interest;
import teaselib.core.ai.perception.HumanPose.PoseAspect;

public class HumanPoseScriptInteraction implements ScriptInteraction {

    public final HumanPoseDeviceInteraction deviceInteraction;

    /**
     * 
     * Await functions for proximity:
     * <li>Towards NEAR
     * <li>Towards AWAY
     * <p>
     * For arrivals, test for pose, then for {@link NotAway}, then for {@link Near}.
     * <p>
     * For departures, test for {@link NotNear}, then for {@link Far} and {@link Away}.
     * 
     */
    public interface Await {
        default boolean await() {
            return await(Long.MAX_VALUE, TimeUnit.SECONDS);
        }

        boolean await(long duration, TimeUnit unit);

        void over(long duration, TimeUnit unit);
    }

    class AwaitImpl implements Await {
        private final Set<Interest> interests;
        private final PoseAspect[] aspects;

        public AwaitImpl(Interest interest, PoseAspect... aspects) {
            this(Collections.singleton(interest), aspects);
        }

        public AwaitImpl(Set<Interest> interests, PoseAspect... aspects) {
            this.interests = interests;
            this.aspects = aspects;
        }

        @Override
        public boolean await(long duration, TimeUnit unit) {
            return deviceInteraction.await(interests, duration, unit, aspects);
        }

        @Override
        public void over(long duration, TimeUnit unit) {
            while (await()) {
                if (!deviceInteraction.awaitNoneOf(interests, duration, unit, aspects)) {
                    break;
                }
            }
        }

    }

    public final Await Presence;

    public final Await FaceToFace;
    public final Await Near;
    public final Await Far;

    public final Await NotFaceToFace;
    public final Await NotNear;
    public final Await NotFar;

    public final Await Absence;

    public HumanPoseScriptInteraction(Script script) {
        this(script.teaseLib.globals
                .get(DeviceInteractionImplementations.class)
                .get(HumanPoseDeviceInteraction.class));
    }

    public HumanPoseScriptInteraction(HumanPoseDeviceInteraction deviceInteraction) {
        this.deviceInteraction = deviceInteraction;

        this.Presence = new AwaitImpl(Interest.Proximity, HumanPose.Proximity.Presence);

        this.FaceToFace = new AwaitImpl(Interest.Proximity, HumanPose.Proximity.Face2Face);
        this.Near = new AwaitImpl(Interest.Proximity, HumanPose.Proximity.Not.Near);
        this.Far = new AwaitImpl(Interest.Proximity, HumanPose.Proximity.Not.Far);

        this.NotFaceToFace = new AwaitImpl(Interest.Proximity, HumanPose.Proximity.Not.Face2Face);
        this.NotNear = new AwaitImpl(Interest.Proximity, HumanPose.Proximity.Not.Near);
        this.NotFar = new AwaitImpl(Interest.Proximity, HumanPose.Proximity.Not.Far);

        this.Absence = new AwaitImpl(Interest.Proximity, HumanPose.Proximity.Not.Far);

    }

    public boolean isActive() {
        return deviceInteraction.isActive();
    }

    public boolean isFaceToFace() {
        return getPose(Proximity).is(HumanPose.Proximity.Face2Face);
    }

    public boolean isNotFaceToFace() {
        return getPose(Proximity).is(HumanPose.Proximity.Not.Face2Face);
    }

    public PoseAspects getPose(Interest interest) {
        return getPose(Collections.singleton(interest));
    }

    public PoseAspects getPose(Set<Interest> interests) {
        return deviceInteraction.getPose(interests);
    }

    public PoseAspects getPose(Interest interest, byte[] image) {
        return getPose(Collections.singleton(interest), image);
    }

    public PoseAspects getPose(Set<Interest> interests, byte[] image) {
        try {
            return deviceInteraction.getPose(interests, image);
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException(e);
        }
    }

    public void setPause(Runnable task) {
        deviceInteraction.setPause(task);
    }

    public void clearPause() {
        deviceInteraction.clearPause();
    }

    public ScriptFunction autoConfirm(BiFunction<Long, TimeUnit, Boolean> pose) {
        return autoConfirm(pose, Long.MAX_VALUE, TimeUnit.SECONDS);
    }

    public ScriptFunction autoConfirm(BiFunction<Long, TimeUnit, Boolean> pose, long duration, TimeUnit unit) {
        return new ScriptFunction(() -> pose.apply(duration, unit)
                ? Answer.yes(pose.toString())
                : Answer.Timeout,
                ScriptFunction.Relation.Confirmation);
    }

    public ScriptFunction autoConfirm(BiConsumer<Long, TimeUnit> pose, long duration, TimeUnit unit) {
        return new ScriptFunction(() -> {
            pose.accept(duration, unit);
            return Answer.Timeout;
        }, ScriptFunction.Relation.Confirmation);
    }

}
