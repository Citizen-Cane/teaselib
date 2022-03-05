package teaselib.core.ai.perception;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import teaselib.Answer;
import teaselib.ScriptFunction;
import teaselib.ScriptFunction.Relation;
import teaselib.core.DeviceInteractionImplementations;
import teaselib.core.Script;
import teaselib.core.ScriptInteraction;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.ai.perception.HumanPose.Interest;

public class HumanPoseScriptInteraction implements ScriptInteraction {

    public final HumanPoseDeviceInteraction deviceInteraction;

    public HumanPoseScriptInteraction(Script script) {
        this.deviceInteraction = script.teaseLib.globals.get(DeviceInteractionImplementations.class)
                .get(HumanPoseDeviceInteraction.class);
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

    public ScriptFunction autoConfirm(Interest interest, HumanPose.PoseAspect... aspects) {
        return autoConfirm(Collections.singleton(interest), aspects);
    }

    public ScriptFunction autoConfirm(Set<Interest> interest, HumanPose.PoseAspect... aspects) {
        return autoConfirm(interest, Long.MAX_VALUE, TimeUnit.SECONDS, aspects);
    }

    public ScriptFunction autoConfirm(Interest interest, long duration, TimeUnit unit,
            HumanPose.PoseAspect... aspects) {
        return autoConfirm(Collections.singleton(interest), duration, unit, aspects);
    }

    public ScriptFunction autoConfirm(Interest interest, long duration, TimeUnit unit, long over,
            HumanPose.PoseAspect... aspects) {
        return autoConfirm(Collections.singleton(interest), duration, unit, aspects);
    }

    public ScriptFunction autoConfirm(Set<Interest> interest, long duration, TimeUnit unit,
            HumanPose.PoseAspect... aspects) {
        return new ScriptFunction(() -> {
            return deviceInteraction.awaitPose(interest, duration, unit, aspects)
                    ? Answer.yes(Arrays.stream(aspects).map(Objects::toString).toList())
                    : Answer.Timeout;
        }, Relation.Confirmation);
    }

}
