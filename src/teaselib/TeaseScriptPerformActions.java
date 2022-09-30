package teaselib;

import static java.util.concurrent.TimeUnit.*;
import static teaselib.core.ai.perception.HumanPose.Interest.*;

import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import teaselib.Message.Type;
import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPoseScriptInteraction;
import teaselib.core.devices.release.KeyReleaseSetup;
import teaselib.util.Item;
import teaselib.util.Items;

public class TeaseScriptPerformActions extends TeaseScript {

    private static final float DELAY_INSTRUCTIONS_EXTEND_FACTOR = 1.5f;
    private static final float DELAY_PROLONGATION_COMMENT_EXTEND_FACTOR = 1.25f;
    private static final double DELAY_INSTRUCTIONS_SECONDS = 5.0f;
    private static final double DELAY_PROLONGATION_COMMENT_SECONDS = 2.0f;

    private final HumanPoseScriptInteraction poseEstimation = interaction(HumanPoseScriptInteraction.class);

    private final BooleanSupplier faceToFace = () -> poseEstimation.getPose(Proximity).is(HumanPose.Proximity.FACE2FACE, HumanPose.Proximity.CLOSE);
    private final ScriptFunction untilFaceToFace = poseEstimation.autoConfirm(Proximity, HumanPose.Proximity.FACE2FACE, HumanPose.Proximity.CLOSE);

    // TODO and not HumanPose.Proximity.CLOSE
    private final ScriptFunction untilNotFaceToFaceOr2s = poseEstimation.autoConfirm(Proximity, 2, SECONDS, HumanPose.Proximity.NotFace2Face);

    // TODO and not HumanPose.Proximity.CLOSE
    private final ScriptFunction untilNotFaceToFace = poseEstimation.autoConfirm(Proximity, HumanPose.Proximity.NotFace2Face);

    private final ScriptFunction untilNotFaceToFaceOver5s = new ScriptFunction(() -> {
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
        prepare(items);
        perform(items, command, commandConfirmation, progressInstructions, completionQuestion,
                completionConfirmation, prolongationExcuse, prolongationComment);
        return items.apply();
    }

    private void prepare(Items items) {
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

    /**
     * Perform an item-related task. The Items may be, for instance, fetched, applied or removed. The items and
     * perform-task are explained via the various message arguments.
     * <p>
     * The performance is usually preceded with an introduction about the purpose of the task.
     * 
     * @param items
     *            The items to perfrom with.
     * @param command
     *            The initial command to start the action. It might mention the items, or how to perform the task, e.g.
     *            to hurry, or to crawl.
     * @param commandConfirmation
     *            Confirmation - dismissed when the slave starts performing, e.g. leaving face2face proximity.
     * @param progressInstructions
     *            The detailed instructions how to perform the task. Repeated when the slave demands more time. The
     *            items to interact with should be mentioned either here, or in the command and the prolongation
     *            message.
     * @param completionQuestion
     *            Asked when the slave returns into face2face proximity.
     * @param completionConfirmation
     *            To confirm completion of the task.
     * @param prolongationExcuse
     *            To ask for more time to complete the task.
     * @param prolongationComment
     *            Message to comment task prolongation.
     * 
     *            The function does not require AI vision. Without a scene capture device, the function falls back to a
     *            simplified version without proximity tracking.
     */
    protected void perform(Items items,
            Message command, Answer commandConfirmation,
            Message progressInstructions,
            Message completionQuestion, Answer completionConfirmation, Answer prolongationExcuse,
            Message prolongationComment) {
        awaitMandatoryCompleted();

        if (poseEstimation.deviceInteraction.isActive()) {
            untilFaceToFace.call();
            say(command);
            awaitMandatoryCompleted();

            boolean playerListening;
            if (faceToFace.getAsBoolean()) {
                if (chat(untilNotFaceToFace, commandConfirmation)) {
                    // prompt dismissed, still face2face
                    playerListening = true;
                } else {
                    // prompt timed out, not face2face anymore -> performing
                    playerListening = false;
                }
            } else {
                // already performing, stop when back -> Listening
                playerListening = false;
            }

            Message progressInstructionsWithDelay = playerListening
                    ? progressInstructions
                    : extendMessageDelays(progressInstructions, DELAY_INSTRUCTIONS_SECONDS, DELAY_INSTRUCTIONS_EXTEND_FACTOR);
            Message prolongationCommentWithDelay = prolongationComment;

            while (true) {
                show(items);
                // TODO pause instructions when the slave is away
                if (playerListening) {
                    append(progressInstructions);
                    awaitMandatoryCompleted();
                    untilFaceToFace.call();
                } else {
                    append(progressInstructionsWithDelay);
                    untilFaceToFace.call();
                    endAll();
                }

                show(items);
                append(completionQuestion);
                // Wait to show the prompt
                untilFaceToFace.call();
                Answer answer = reply(untilNotFaceToFaceOver5s, completionConfirmation, prolongationExcuse);
                if (answer == Answer.Timeout) {
                    playerListening = true;
                    continue;
                } else if (answer == completionConfirmation) {
                    break;
                } else {
                    say(prolongationComment);
                    awaitMandatoryCompleted();
                    // True if player is still looking at top -> repeat instructions
                    playerListening = untilNotFaceToFaceOr2s.call() == Answer.Timeout;
                    if (!playerListening) {
                        progressInstructionsWithDelay = extendMessageDelays(progressInstructionsWithDelay, DELAY_INSTRUCTIONS_SECONDS,
                                DELAY_INSTRUCTIONS_EXTEND_FACTOR);
                        prolongationCommentWithDelay = extendMessageDelays(prolongationCommentWithDelay, DELAY_PROLONGATION_COMMENT_SECONDS,
                                DELAY_PROLONGATION_COMMENT_EXTEND_FACTOR);
                    }
                }
            }
        } else {
            say(command);
            chat(timeoutWithAutoConfirmation(5), commandConfirmation);
            while (true) {
                show(items);
                append(progressInstructions);
                append(completionQuestion);
                Answer answer = reply(completionConfirmation, prolongationExcuse);
                if (answer == completionConfirmation) {
                    break;
                } else {
                    say(prolongationComment);
                }
            }
        }
    }

    private static Message extendMessageDelays(Message message, double seconds, float factor) {
        Message extended = new Message(message.actor);
        boolean injectMissingDelay = false;
        for (var part : message) {
            if (part.type == Type.Text) {
                if (injectMissingDelay) {
                    extended.add(Type.Delay, DELAY_INSTRUCTIONS_SECONDS);
                } else {
                    injectMissingDelay = true;
                }
                extended.add(part);
            } else if (part.type == Type.Delay) {
                injectMissingDelay = false;
                String[] values = part.value.split(" ");
                if (values.length > 0 && values.length < 3) {
                    String value = Stream.of(values).map(Float::parseFloat).filter(f -> f > 0.0f).map(f -> f * factor).map(f -> Float.toString(f))
                            .collect(Collectors.joining(" "));
                    extended.add(Type.Delay, value);
                } else {
                    extended.add(part);
                }
            } else {
                if (injectMissingDelay) {
                    injectMissingDelay = false;
                    extended.add(Type.Delay, DELAY_INSTRUCTIONS_SECONDS);
                }
                extended.add(part);
            }
        }
        if (extended.last().type != Type.Delay) {
            extended.add(Type.Delay, DELAY_INSTRUCTIONS_SECONDS);
        }
        return extended;
    }
}