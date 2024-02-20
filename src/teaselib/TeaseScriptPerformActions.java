package teaselib;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import teaselib.Message.Type;
import teaselib.core.ai.perception.HumanPoseScriptInteraction;
import teaselib.core.devices.release.KeyReleaseSetup;
import teaselib.util.Item;
import teaselib.util.Items;

public class TeaseScriptPerformActions extends TeaseScript {

    private static final float DELAY_INSTRUCTIONS_EXTEND_FACTOR = 1.5f;
    private static final float DELAY_PROLONGATION_COMMENT_EXTEND_FACTOR = 1.25f;
    private static final double DELAY_INSTRUCTIONS_SECONDS = 5.0f;
    private static final double DELAY_PROLONGATION_COMMENT_SECONDS = 2.0f;

    private final HumanPoseScriptInteraction pose = interaction(HumanPoseScriptInteraction.class);

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
     *            The items to perform with.
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

        if (pose.isActive()) {
            pose.FaceToFace.await();

            say(command);
            awaitMandatoryCompleted();

            boolean playerListening;
            if (pose.isFaceToFace()) {
                if (chat(pose.autoConfirm(pose.NotFaceToFace::await), commandConfirmation)) {
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
                // TODO pause instructions when the slave is away, continue when back
                if (playerListening) {
                    append(progressInstructions);
                    awaitMandatoryCompleted();
                    pose.FaceToFace.await();
                } else {
                    append(progressInstructionsWithDelay);
                    // Player might look face2face to check the instructions, or after completIng the task
                    // - how to handle?
                    pose.FaceToFace.over(2, SECONDS);
                    // TODO Await speech paragraph end, as interrupting speech in the middle of a word sounds
                    // unrealistic
                    endAll();
                    // -> manually loop through message, replacing delay with pose.over(...)
                    // TODO add message iterator that calls BooleanSupplier() each time and stops on false
                }

                show(items);
                append(completionQuestion);
                // ensure player is face2face in order to show until player looks away
                pose.FaceToFace.await();
                Answer answer = reply(pose.autoConfirm(pose.NotFaceToFace::over, 5, SECONDS), completionConfirmation, prolongationExcuse);
                if (answer == Answer.Timeout) {
                    playerListening = true;
                    // workaround for restoring the command for the next iteration of the instructional loop
                    // TODO make this paragraph appear before every append()
                    show(command);
                    continue;
                } else if (answer == completionConfirmation) {
                    break;
                } else {
                    say(prolongationComment);
                    awaitMandatoryCompleted();
                    // True if player is still looking at top -> repeat instructions
                    playerListening = !pose.NotFaceToFace.await(2, SECONDS);
                    if (!playerListening) {
                        progressInstructionsWithDelay = extendMessageDelays(
                                progressInstructionsWithDelay, DELAY_INSTRUCTIONS_SECONDS, DELAY_INSTRUCTIONS_EXTEND_FACTOR);
                        prolongationCommentWithDelay = extendMessageDelays(
                                prolongationCommentWithDelay, DELAY_PROLONGATION_COMMENT_SECONDS, DELAY_PROLONGATION_COMMENT_EXTEND_FACTOR);
                    }
                }
            }
        } else {
            say(command);
            chat(timeoutWithAutoConfirmation(5), commandConfirmation);
            Message progressInstructionsWithDelay = extendMessageDelays(progressInstructions, DELAY_INSTRUCTIONS_SECONDS, DELAY_INSTRUCTIONS_EXTEND_FACTOR);
            Message prolongationCommentWithDelay = prolongationComment;
            while (true) {
                show(items);
                append(progressInstructionsWithDelay);
                append(completionQuestion);
                Answer answer = reply(completionConfirmation, prolongationExcuse);
                if (answer == completionConfirmation) {
                    break;
                } else {
                    say(prolongationCommentWithDelay);
                    progressInstructionsWithDelay = extendMessageDelays(
                            progressInstructionsWithDelay, DELAY_INSTRUCTIONS_SECONDS, DELAY_INSTRUCTIONS_EXTEND_FACTOR);
                    prolongationCommentWithDelay = extendMessageDelays(
                            prolongationCommentWithDelay, DELAY_PROLONGATION_COMMENT_SECONDS, DELAY_PROLONGATION_COMMENT_EXTEND_FACTOR);
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
                    extended.add(Type.Delay, seconds);
                }
                extended.add(part);
            }
        }
        if (extended.last().type != Type.Delay) {
            extended.add(Type.Delay, seconds);
        }
        return extended;
    }
}