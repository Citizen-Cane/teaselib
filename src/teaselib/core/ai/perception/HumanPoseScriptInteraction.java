package teaselib.core.ai.perception;

import static java.util.Collections.singleton;
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
        boolean await(long duration, TimeUnit unit);

        default boolean await() {
            return await(Long.MAX_VALUE, TimeUnit.SECONDS);
        }

        void over(long duration, TimeUnit unit);
    }

    public final Await FaceToFace;
    public final Await NotFaceToFace;

    public final Await Near;
    public final Await NotNear;

    public final Await Far;
    public final Await NotFar;

    public final Await Away;
    public final Await NotAway;

    public HumanPoseScriptInteraction(Script script) {
        this(script.teaseLib.globals
                .get(DeviceInteractionImplementations.class)
                .get(HumanPoseDeviceInteraction.class));
    }

    public HumanPoseScriptInteraction(HumanPoseDeviceInteraction deviceInteraction) {
        this.deviceInteraction = deviceInteraction;

        this.FaceToFace = new Await() {
            @Override
            public boolean await(long duration, TimeUnit unit) {
                return deviceInteraction.awaitPose(
                        singleton(Proximity), duration, unit,
                        HumanPose.Proximity.FACE2FACE,
                        HumanPose.Proximity.CLOSE);
            }

            @Override
            public void over(long duration, TimeUnit unit) {
                while (await()) {
                    if (!NotFaceToFace.await(duration, unit)) {
                        break;
                    }
                }
            }
        };

        this.NotFaceToFace = new Await() {
            @Override
            public boolean await(long duration, TimeUnit unit) {
                return deviceInteraction.awaitPose(
                        singleton(Proximity), duration, unit,
                        HumanPose.Proximity.NotFace2Face);
                // TODO await absent pose
            }

            @Override
            public void over(long duration, TimeUnit unit) {
                while (await()) {
                    if (!FaceToFace.await(duration, unit)) {
                        break;
                    }
                }
            }
        };

        this.Near = new Await() {
            @Override
            public boolean await(long duration, TimeUnit unit) {
                return deviceInteraction.awaitPose(
                        singleton(Proximity), duration, unit,
                        HumanPose.Proximity.NEAR,
                        HumanPose.Proximity.FACE2FACE,
                        HumanPose.Proximity.CLOSE);
            }

            @Override
            public void over(long duration, TimeUnit unit) {
                while (await()) {
                    if (!NotNear.await(duration, unit)) {
                        break;
                    }
                }
            }
        };

        this.NotNear = new Await() {
            @Override
            public boolean await(long duration, TimeUnit unit) {
                return deviceInteraction.awaitPose(
                        singleton(Proximity), duration, unit,
                        HumanPose.Proximity.FAR,
                        HumanPose.Proximity.AWAY);
                // TODO await absent pose
            }

            @Override
            public void over(long duration, TimeUnit unit) {
                while (await()) {
                    if (!Near.await(duration, unit)) {
                        break;
                    }
                }
            }
        };

        this.Far = new Await() {
            @Override
            public boolean await(long duration, TimeUnit unit) {
                return deviceInteraction.awaitPose(
                        singleton(Proximity), duration, unit,
                        HumanPose.Proximity.FAR,
                        HumanPose.Proximity.AWAY);
                // TODO await absent pose
            }

            @Override
            public void over(long duration, TimeUnit unit) {
                while (await()) {
                    if (!NotFar.await(duration, unit)) {
                        break;
                    }
                }
            }
        };

        this.NotFar = new Await() {
            @Override
            public boolean await(long duration, TimeUnit unit) {
                return deviceInteraction.awaitPose(
                        singleton(Proximity), duration, unit,
                        HumanPose.Proximity.NEAR,
                        HumanPose.Proximity.FACE2FACE,
                        HumanPose.Proximity.CLOSE);
            }

            @Override
            public void over(long duration, TimeUnit unit) {
                while (await()) {
                    if (!Far.await(duration, unit)) {
                        break;
                    }
                }
            }
        };

        this.Away = new Await() {
            @Override
            public boolean await(long duration, TimeUnit unit) {
                return deviceInteraction.awaitPose(
                        singleton(Proximity), duration, unit,
                        HumanPose.Proximity.AWAY);
                // TODO await absent pose
            }

            @Override
            public void over(long duration, TimeUnit unit) {
                while (await()) {
                    if (!NotAway.await(duration, unit)) {
                        break;
                    }
                }
            }
        };

        this.NotAway = new Await() {
            @Override
            public boolean await(long duration, TimeUnit unit) {
                return deviceInteraction.awaitPose(
                        singleton(Proximity), duration, unit,
                        HumanPose.Proximity.FAR,
                        HumanPose.Proximity.NEAR,
                        HumanPose.Proximity.FACE2FACE,
                        HumanPose.Proximity.CLOSE);
            }

            @Override
            public void over(long duration, TimeUnit unit) {
                while (await()) {
                    if (!Away.await(duration, unit)) {
                        break;
                    }
                }
            }
        };

    }

    public boolean isActive() {
        return deviceInteraction.isActive();
    }

    public boolean isFaceToFace() {
        return getPose(Proximity).is(HumanPose.Proximity.FACE2FACE, HumanPose.Proximity.CLOSE);
    }

    public boolean isNotFaceToFace() {
        return getPose(Proximity).is(HumanPose.Proximity.NotFace2Face);
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
