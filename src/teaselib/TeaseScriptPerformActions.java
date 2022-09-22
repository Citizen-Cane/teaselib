package teaselib;

import static java.util.concurrent.TimeUnit.*;
import static teaselib.core.ai.perception.HumanPose.Interest.*;

import java.util.function.BooleanSupplier;

import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPoseScriptInteraction;
import teaselib.core.devices.release.KeyReleaseSetup;
import teaselib.util.Item;
import teaselib.util.Items;

public class TeaseScriptPerformActions extends TeaseScript {

    public TeaseScriptPerformActions(TeaseScript script) {
        super(script);
    }

    /**
     * Fetch items, but don't apply them (yet)
     */
    public void fetch(Item item,
            Message command, Answer commandConfirmation,
            Message progressInstructions,
            Message completionQuestion, Answer completionConfirmation,
            Answer prolongationExcuse, Message prolongationComment) {
        fetch(items(item), command, commandConfirmation, progressInstructions, completionQuestion,
                completionConfirmation, prolongationExcuse, prolongationComment);
    }

    public void fetch(Items items,
            Message command, Answer commandConfirmation,
            Message progressInstructions,
            Message completionQuestion, Answer completionConfirmation,
            Answer prolongationExcuse, Message prolongationComment) {
        show(items);
        perform(items, command, commandConfirmation, progressInstructions, completionQuestion,
                completionConfirmation, prolongationExcuse, prolongationComment);
    }

    /**
     * Apply items at the end of the call
     */
    public final State.Options apply(Item item,
            Message command, Answer commandConfirmation,
            Message progressInstructions, Message completionQuestion,
            Answer completionConfirmation, Answer prolongationExcuse,
            Message prolongationComment) {
        return apply(items(item), command, commandConfirmation, progressInstructions, completionQuestion,
                completionConfirmation, prolongationExcuse, prolongationComment);
    }

    public State.Options apply(Items items,
            Message command, Answer commandConfirmation,
            Message progressInstructions,
            Message completionQuestion, Answer completionConfirmation,
            Answer prolongationExcuse, Message prolongationComment) {

        KeyReleaseSetup keyRelease = interaction(KeyReleaseSetup.class);
        if (keyRelease != null) {
            if (!keyRelease.isPrepared(items) && keyRelease.canPrepare(items)) {
                keyRelease.prepare(items, KeyReleaseSetup.DefaultInstructions);
            } else {
                show(items);
            }
        } else {
            show(items);
        }

        perform(items, command, commandConfirmation, progressInstructions, completionQuestion,
                completionConfirmation, prolongationExcuse, prolongationComment);

        return items.apply();
    }

    public final void remove(Item item,
            Message command, Answer commandConfirmation,
            Message progressInstructions,
            Message completionQuestion, Answer completionConfirmation,
            Answer prolongationExcuse, Message prolongationComment) {
        remove(items(item), command, commandConfirmation, progressInstructions, completionQuestion,
                completionConfirmation, prolongationExcuse, prolongationComment);
    }

    public void remove(Items items,
            Message command, Answer command1stConfirmation,
            Message progressInstructions, Message completionQuestion,
            Answer completionConfirmation, Answer prolongationExcuse,
            Message prolongationComment) {

        items.remove();
        perform(items, command, command1stConfirmation, progressInstructions, completionQuestion,
                completionConfirmation, prolongationExcuse, prolongationComment);
    }

    protected void perform(Items items,
            Message command, Answer commandConfirmation,
            Message progressInstructions,
            Message completionQuestion, Answer completionConfirmation, Answer prolongationExcuse,
            Message prolongationComment) {
        awaitMandatoryCompleted();

        HumanPoseScriptInteraction poseEstimation = interaction(HumanPoseScriptInteraction.class);
        if (poseEstimation.deviceInteraction.isActive()) {
            BooleanSupplier faceToFace = () -> poseEstimation.getPose(Proximity).is(HumanPose.Proximity.FACE2FACE,
                    HumanPose.Proximity.CLOSE);
            var untilFaceToFace = poseEstimation.autoConfirm(Proximity, HumanPose.Proximity.FACE2FACE,
                    HumanPose.Proximity.CLOSE);
            var untilNotFaceToFaceOr5s = poseEstimation.autoConfirm(Proximity, 5, SECONDS,
                    // TODO and not HumanPose.Proximity.CLOSE
                    HumanPose.Proximity.NotFace2Face);
            var untilNotFaceToFace = poseEstimation.autoConfirm(Proximity,
                    // TODO and not HumanPose.Proximity.CLOSE
                    HumanPose.Proximity.NotFace2Face);

            var untilNotFaceToFaceOver5s = new ScriptFunction(() -> {
                Answer notFace2Face;
                while ((notFace2Face = poseEstimation.autoConfirm(Proximity, HumanPose.Proximity.NotFace2Face)
                        .call()) != Answer.Timeout) {
                    if (poseEstimation.autoConfirm(Proximity, 3, SECONDS, HumanPose.Proximity.FACE2FACE)
                            .call() == Answer.Timeout) {
                        break;
                    }
                }
                return notFace2Face;
            });

            untilFaceToFace.call();
            say(command);
            awaitMandatoryCompleted();

            boolean explainAll;
            if (faceToFace.getAsBoolean()) {
                if (chat(untilNotFaceToFace, commandConfirmation)) {
                    // prompt dismissed, still face2face
                    explainAll = true;
                } else {
                    // prompt timed out, not face2face anymore
                    explainAll = false;
                }
            } else {
                // already performing, stop when back
                explainAll = false;
            }

            while (true) {
                show(items);
                if (explainAll) {
                    say(progressInstructions);
                    awaitMandatoryCompleted();
                    untilFaceToFace.call();
                } else {
                    append(progressInstructions);
                    untilFaceToFace.call();
                    endAll();
                }

                show(items);
                append(completionQuestion);
                // Wait to show the prompt
                untilFaceToFace.call();
                Answer answer = reply(untilNotFaceToFaceOver5s, completionConfirmation, prolongationExcuse);
                if (answer == Answer.Timeout) {
                    explainAll = true;
                    continue;
                } else if (answer == completionConfirmation) {
                    break;
                } else {
                    say(prolongationComment);
                    explainAll = untilNotFaceToFaceOr5s.call() == Answer.Timeout;
                }
            }
        } else {
            say(command);
            chat(timeoutWithAutoConfirmation(5), commandConfirmation);
            while (true) {
                append(progressInstructions);
                show(items);
                append(completionQuestion);
                Answer answer = reply(completionConfirmation, prolongationExcuse);
                if (answer == completionConfirmation) {
                    break;
                } else {
                    say(prolongationComment);
                    append(Message.Delay5to10s);
                }
            }
        }
    }
}