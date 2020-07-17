package teaselib.core.ai.perception;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import teaselib.Answer;
import teaselib.ScriptFunction;
import teaselib.ScriptFunction.Relation;
import teaselib.core.DeviceInteractionImplementations;
import teaselib.core.Script;
import teaselib.core.ScriptInteraction;
import teaselib.core.ai.perception.HumanPose.Interests;
import teaselib.core.ai.perception.HumanPoseDeviceInteraction.Pose;

public class HumanPoseScriptInteraction implements ScriptInteraction {

    public final HumanPoseDeviceInteraction deviceInteraction;

    public HumanPoseScriptInteraction(Script script) {
        this.deviceInteraction = script.teaseLib.globals.get(DeviceInteractionImplementations.class)
                .get(HumanPoseDeviceInteraction.class);
    }

    public Pose getPose(Interests interests) {
        return deviceInteraction.getPose(interests);
    }

    public ScriptFunction autoConfirm(Interests interest, HumanPose.PoseAspect... aspects) {
        return autoConfirm(interest, Long.MAX_VALUE, TimeUnit.SECONDS, aspects);
    }

    public ScriptFunction autoConfirm(Interests interest, long time, TimeUnit unit, HumanPose.PoseAspect... aspects) {
        return new ScriptFunction(() -> {
            return deviceInteraction.awaitPose(interest, time, unit, aspects)
                    ? Answer.yes(Arrays.stream(aspects).map(Objects::toString).collect(Collectors.toList()))
                    : Answer.Timeout;
        }, Relation.Confirmation);
    }

}
