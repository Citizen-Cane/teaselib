package teaselib;

import static teaselib.core.ai.perception.HumanPose.Interest.Proximity;
import static teaselib.core.ai.perception.HumanPose.Interest.Status;
import static teaselib.core.ai.perception.HumanPose.Status.Available;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Answer.Meaning;
import teaselib.ScriptFunction.AnswerOverride;
import teaselib.ScriptFunction.Relation;
import teaselib.core.ResourceLoader;
import teaselib.core.Script;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.TeaseLib;
import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPoseScriptInteraction;
import teaselib.core.events.Event;
import teaselib.core.events.EventSource;
import teaselib.core.media.MediaRenderer;
import teaselib.core.media.RenderDelay;
import teaselib.core.media.RenderDesktopItem;
import teaselib.core.media.RenderSound;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognition.TimeoutBehavior;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.ui.InputMethods;
import teaselib.core.ui.Intention;
import teaselib.core.ui.SpeechRecognitionInputMethod;
import teaselib.core.util.ExceptionUtil;
import teaselib.functional.CallableScript;
import teaselib.functional.RunnableScript;
import teaselib.util.Item;
import teaselib.util.Items;

public abstract class TeaseScript extends TeaseScriptMath {
    private static final Logger logger = LoggerFactory.getLogger(TeaseScript.class);

    /**
     * Create a sub-script with the same actor as the parent.
     * 
     * @param script
     *            The script to share resources with
     */
    public TeaseScript(Script script) {
        super(script, script.actor);
    }

    /**
     * Create a sub-script with a different actor
     * 
     * @param script
     *            The script to share resources with
     * @param actor
     *            If both script and actor have the same locale, the speech recognizer is shared by both scripts
     */
    public TeaseScript(TeaseScript script, Actor actor) {
        super(script, actor);
    }

    /**
     * Create a new script, with a distinct actor
     * 
     * @param teaseLib
     * @param actor
     * @param namespace
     */
    public TeaseScript(TeaseLib teaseLib, ResourceLoader resources, Actor actor, String namespace) {
        super(teaseLib, resources, actor, namespace);
    }

    /**
     * Renders the image denoted by the resource path. The image will not be displayed immediately but during the next
     * message rendering. This is because if no image is specified, an image of the dominant character will be used.
     * 
     * @param path
     *            The resource path to the image
     */
    public void setImage(String path) {
        if (path == null) {
            displayImage = Message.NoImage;
        } else if (path.equalsIgnoreCase(Message.ActorImage)) {
            displayImage = Message.ActorImage;
        } else if (path.equalsIgnoreCase(Message.NoImage)) {
            displayImage = Message.NoImage;
        } else {
            displayImage = path;
        }
    }

    public void showDesktopItem(String path) {
        if (Boolean.parseBoolean(teaseLib.config.get(Config.Render.InstructionalImages))) {
            try {
                MediaRenderer desktopItem = new RenderDesktopItem(teaseLib, resources, path);
                scriptRenderer.queueRenderer(desktopItem);
            } catch (IOException e) {
                if (Boolean.parseBoolean(teaseLib.config.get(Config.Debug.StopOnRenderError))) {
                    throw ExceptionUtil.asRuntimeException(e);
                } else {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    public void setBackgroundSound(String path) {
        if (Boolean.parseBoolean(teaseLib.config.get(Config.Render.Sound))) {
            try {
                scriptRenderer.queueBackgroundRenderer(new RenderSound(resources, path, teaseLib));
            } catch (IOException e) {
                ExceptionUtil.handleException(e, teaseLib.config, logger);
            }
        }
    }

    public void setSound(String path) {
        if (Boolean.parseBoolean(teaseLib.config.get(Config.Render.Sound))) {
            try {
                scriptRenderer.queueRenderer(new RenderSound(resources, path, teaseLib));
            } catch (IOException e) {
                ExceptionUtil.handleException(e, teaseLib.config, logger);
            }
        }
    }

    public MediaRenderer.Threaded getSound(String path) {
        if (Boolean.parseBoolean(teaseLib.config.get(Config.Render.Sound))) {
            try {
                return new RenderSound(resources, path, teaseLib);
            } catch (IOException e) {
                ExceptionUtil.handleException(e, teaseLib.config, logger);
            }
        }
        return new RenderDelay(0, teaseLib);
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    /**
     * Wait the requested numbers of seconds after displaying a message.
     * 
     * @param seconds
     *            How long to wait.
     */
    public void setDuration(int seconds) {
        scriptRenderer.queueRenderer(new RenderDelay(seconds, teaseLib));
    }

    public final void prepend(String... message) {
        prepend(new Message(actor, message));
    }

    public final void prepend(List<String> message) {
        prepend(new Message(actor, message));
    }

    public final void prepend(Message message) {
        prependMessage(message);
    }

    public void say(String... message) {
        say(new Message(actor, message));
    }

    public void say(List<String> message) {
        say(new Message(actor, message));
    }

    public void say(Message message) {
        renderMessage(message, true);
    }

    public final void append(String... message) {
        append(new Message(actor, message));
    }

    public final void append(List<String> message) {
        append(new Message(actor, message));
    }

    public final void append(Message message) {
        appendMessage(message);
    }

    public final void replace(String... message) {
        replace(new Message(actor, message));
    }

    public final void replace(List<String> message) {
        replace(new Message(actor, message));
    }

    public final void replace(Message message) {
        replaceMessage(message);
    }

    /**
     * Show instructional text, this is not spoken, just displayed.
     * 
     * @param message
     *            The text to be displayed, or null to display no message at all
     */
    public void show(String text) {
        boolean emptyMessage = text == null || text.isEmpty();
        show(emptyMessage ? new Message(actor) : new Message(actor, text));
    }

    public void show(String... message) {
        show(new Message(actor, message));
    }

    public void show(Message message) {
        renderMessage(message, false);
    }

    /**
     * Show item images to hint slave what he or she should put on. Actors may reference to the shown hints in theier
     * dialog messages, which allows for greater flexibility in script dialogs - its just more natural to show the item
     * instead of describing it in every detail.
     * 
     * @param item
     *            The items to hint / display.
     */
    public void show(Item... items) {
        show(Arrays.asList(items));
    }

    public void show(Enum<?>... items) {
        show(items(items));
    }

    public void show(List<Item> items) {
        show(new Items(items));
    }

    public void show(Items items) {
        // TODO Implement show items when with current say/show/reply statement
    }

    /**
     * Show an intertitle ({@link https://en.wikipedia.org/wiki/Intertitle}) to reveal information or give instructions
     * that are encessary to bring the script foraward, but aren't suitable to be given by a character.
     * <p>
     * For instance if you are to accidently spill coffee on the floor, the command to do shouldn't be given by any of
     * the actors you're interacting with, as that would break their character.
     * 
     * The command works the same as the {@link #show(String ...)} command, but there will be no image associated with
     * it.
     * 
     * @param text
     *            The text to show with the intertitle.
     */
    public void showInterTitle(String... text) {
        renderIntertitle(text);
    }

    /**
     * Displays the requested choices in the user interface after the mandatory parts of all renderers have been
     * completed. This means especially that all text has been displayed and spoken.
     * 
     * @param text
     *            The prompts to be displayed in the user interface
     * @return The choice object that has been selected by the user.
     */
    public final String reply(List<String> text) {
        return showChoices(Answers.of(text), null).text.get(0);
    }

    /**
     * Displays the requested choices in the user interface after the mandatory parts of all renderers have been
     * completed. This means especially that all text has been displayed and spoken.
     * 
     * @param answer
     *            The first prompt to be displayed by the user interface.
     * @param more
     *            More prompts to be displayed by the user interface
     * @return The choice object that has been selected by the user.
     */
    public final String reply(String answer, String... more) {
        return showChoices(Answers.of(answer, more)).text.get(0);
    }

    /**
     * Displays the requested choices in the user interface after the mandatory parts of all renderers have been
     * completed. This means especially that all text has been displayed and spoken.
     * 
     * @param text
     *            The prompts to be displayed in the user interface
     * @return The choice object that has been selected by the user, or {@link TeaseScript#Timeout} if the script
     *         function completes.
     */

    public final String reply(ScriptFunction scriptFunction, List<String> text) {
        return showChoices(Answers.of(text), scriptFunction).text.get(0);
    }

    public final String reply(RunnableScript script, List<String> text) {
        return showChoices(Answers.of(text), new ScriptFunction(script)).text.get(0);
    }

    public final String reply(CallableScript<String> script, List<String> text) {
        CallableScript<Answer> stringAdapter = () -> Answer.resume(script.call());
        return showChoices(Answers.of(text), new ScriptFunction(stringAdapter)).text.get(0);
    }

    /**
     * Displays the requested choices in the user interface after the mandatory parts of all renderers have been
     * completed. This means especially that all text has been displayed and spoken.
     * 
     * @param text
     *            The first prompt to be displayed by the user interface.
     * @param more
     *            More prompts to be displayed by the user interface
     * @return The choice object that has been selected by the user, or {@link TeaseScript#Timeout} if the script
     *         function completes.
     */

    public final String reply(ScriptFunction scriptFunction, String text, String... more) {
        return showChoices(Answers.of(text, more), scriptFunction).text.get(0);
    }

    public final String reply(RunnableScript script, String text, String... more) {
        return showChoices(Answers.of(text, more), new ScriptFunction(script)).text.get(0);
    }

    public final String reply(CallableScript<String> script, String text, String... more) {
        CallableScript<Answer> stringAdapter = () -> Answer.resume(script.call());
        return showChoices(Answers.of(text, more), new ScriptFunction(stringAdapter)).text.get(0);
    }

    @SuppressWarnings("resource")
    protected Answer awaitTimeout(long seconds, SpeechRecognition.TimeoutBehavior timeoutBehavior) {
        SpeechRecognitionInputMethod inputMethod = teaseLib.globals.get(InputMethods.class)
                .get(SpeechRecognitionInputMethod.class);
        AtomicBoolean ignoreTimeoutInDubioMitius = new AtomicBoolean(false);

        EventSource<SpeechRecognizedEventArgs> speechDetectedEventSource;
        Event<SpeechRecognizedEventArgs> recognitionRejected;
        if (timeoutBehavior == TimeoutBehavior.InDubioMitius) {
            speechDetectedEventSource = inputMethod.events.speechDetected;
            Thread scriptFunctionThread = Thread.currentThread();
            recognitionRejected = eventArgs -> {
                if (!ignoreTimeoutInDubioMitius.get()) {
                    logger.info("-{} - : timeout disabled {}", scriptFunctionThread.getName(), timeoutBehavior);
                    ignoreTimeoutInDubioMitius.set(true);
                }
            };
            speechDetectedEventSource.add(recognitionRejected);
        } else {
            speechDetectedEventSource = null;
            recognitionRejected = null;
        }

        try {
            teaseLib.sleep(seconds, TimeUnit.SECONDS);
            if (timeoutBehavior != TimeoutBehavior.InDubioContraReum
                    && scriptRenderer.audioSync.speechRecognitionInProgress()) {
                scriptRenderer.audioSync.completeSpeechRecognition();
            }
        } finally {
            if (speechDetectedEventSource != null && recognitionRejected != null) {
                speechDetectedEventSource.remove(recognitionRejected);
            }
        }

        Answer result;
        if (ignoreTimeoutInDubioMitius.get()) {
            logger.info(" accepted {}", timeoutBehavior);
            result = null;
        } else {
            logger.info("Prompt result timeout {} ", timeoutBehavior);
            result = Answer.Timeout;
        }

        return result;
    }

    /**
     * Wait until the timeout duration has elapsed, wait for ongoing speech recognition to complete, dismiss the buttons
     * and return {@link teaselib.Answer#Timeout} instead of a choice
     * 
     * The function waits until speech recognition is completed before marking the choice as "Timed out", because the
     * user has usually completed the requested action before uttering a choice, and because speaking takes more time
     * than pressing a button.
     * 
     * @param seconds
     *            The timeout duration
     * @param timeoutBehavior
     *            How speech recognition is handled when the timeout has been reached
     * @return A script function that accomplishes the described behavior.
     */

    public ScriptFunction timeout(long seconds) {
        return timeout(seconds, TimeoutBehavior.InDubioProDuriore);
    }

    public ScriptFunction timeout(long seconds, SpeechRecognition.TimeoutBehavior timeoutBehavior) {
        return new ScriptFunction(() -> awaitTimeout(seconds, timeoutBehavior), Relation.Autonomous);
    }

    /**
     * Wait until the timeout duration has elapsed, wait for ongoing speech recognition to complete, then wait until the
     * user makes a choice and return {@link teaselib.ScriptFunction#Timeout} instead of the users' choice.
     * <p>
     * While this behavior can be implemented with a standard button, the function waits until speech recognition is
     * completed before marking the choice as "Timed out", because the user has usually completed the requested action
     * before uttering a choice, and because speaking takes more time than pressing a button.
     * 
     * @param seconds
     *            The timeout duration
     * @param timoutBehavior
     *            How speech recognition is handled when the timeout has been reached
     * @return A script function that accomplishes the described behavior.
     */

    public ScriptFunction timeoutWithConfirmation(long seconds) {
        return timeoutWithConfirmation(seconds, TimeoutBehavior.InDubioProDuriore);
    }

    public ScriptFunction timeoutWithConfirmation(long seconds, SpeechRecognition.TimeoutBehavior timeoutBehavior) {
        return new ScriptFunction(() -> {
            Answer result = awaitTimeout(seconds, timeoutBehavior);
            try {
                sleep(ScriptFunction.Infinite, TimeUnit.SECONDS);
            } catch (ScriptInterruptedException e) {
                throw new AnswerOverride(Answer.Timeout);
            }
            return result;
        }, Relation.Confirmation);
    }

    /**
     * Wait until the timeout duration has elapsed, and wait for ongoing speech recognition to complete. If the duration
     * elapses, return {@link teaselib.ScriptFunction#Timeout} instead of waiting for user input.
     * <p>
     * 
     * The function behaves like a normal prompt, but the buttons are automatically dismissed when the duration expires.
     * 
     * @param seconds
     *            The timeout duration
     * @param timoutBehavior
     *            How speech recognition is handled when the timeout has been reached
     * @return A script function that accomplishes the described behavior.
     */
    public ScriptFunction timeoutWithAutoConfirmation(long seconds) {
        return timeoutWithAutoConfirmation(seconds, TimeoutBehavior.InDubioProDuriore);
    }

    public ScriptFunction timeoutWithAutoConfirmation(long seconds, SpeechRecognition.TimeoutBehavior timeoutBehavior) {
        return new ScriptFunction(() -> awaitTimeout(seconds, timeoutBehavior), Relation.Confirmation);
    }

    /**
     * Displays the requested choices in the user interface after the mandatory parts of all renderers have been
     * completed. This means especially that all text has been displayed and spoken.
     * 
     * @param answer
     *            The first prompt to be displayed by the user interface.
     * @param more
     *            More prompts to be displayed by the user interface
     * @return The index of the choice object in the argument list that has been selected by the user.
     */
    public final int replyIndex(String answer, String... more) {
        return replyIndex(Answers.of(answer, more));
    }

    /**
     * Displays the requested choices in the user interface after the mandatory parts of all renderers have been
     * completed. This means especially that all text has been displayed and spoken.
     * 
     * @param answer
     *            The prompts to be displayed in the user interface
     * @return The index of the choice object in the {@code choices} list that has been selected by the user.
     */
    public final int replyIndex(List<String> answers) {
        return answers.indexOf(reply(answers));
    }

    public final int replyIndex(Answers answers) {
        return answers.indexOf(showChoices(answers));
    }

    public final Answer reply(Answer... answers) {
        return showChoices(Arrays.asList(answers));
    }

    public final Answer reply(Answers answers) {
        return showChoices(answers);
    }

    public final Answer reply(ScriptFunction scriptFunction, Answer answer, Answer... more) {
        return showChoices(Answers.of(answer, more), scriptFunction);
    }

    public final Answer reply(RunnableScript script, Answer answer, Answer... more) {
        return showChoices(Answers.of(answer, more), new ScriptFunction(script));
    }

    public final Answer reply(CallableScript<Answer> script, Answer answer, Answer... more) {
        return showChoices(Answers.of(answer, more), new ScriptFunction(script));
    }

    public final Answer reply(ScriptFunction scriptFunction, Answers answers) {
        return showChoices(answers, scriptFunction);
    }

    public final Answer reply(Intention intention, Answer... answers) {
        return showChoices(Arrays.asList(answers), null, intention);
    }

    public final String reply(Intention intention, String answer) {
        return showChoices(Answers.of(answer), null, intention).text.get(0);
    }

    public final String reply(Intention intention, List<String> answers) {
        return showChoices(Answers.of(answers), null, intention).text.get(0);
    }

    public final String reply(ScriptFunction scriptFunction, Intention intention, List<String> answers) {
        return showChoices(Answers.of(answers), scriptFunction, intention).text.get(0);
    }

    /**
     * Displays the requested choices in the user interface after the mandatory parts of all renderers have been
     * completed. This means especially that all text has been displayed and spoken.
     * 
     * @param yes
     *            The first prompt.
     * @param no
     *            The second prompt.
     * @return True if {@code yes} has been selected, false if {@code no} has been selected.
     */
    public final boolean askYN(String yes, String no) {
        return askYN(Answer.yes(yes), Answer.no(no));
    }

    public final boolean askYN(ScriptFunction scriptFunction, String yes, String no) {
        return askYN(scriptFunction, Answer.yes(yes), Answer.no(no));
    }

    public final boolean askYN(RunnableScript script, String yes, String no) {
        return askYN(new ScriptFunction(script), Answer.yes(yes), Answer.no(no));
    }

    public final boolean askYN(Answer yes, Answer no) {
        return askYN(null, yes, no);
    }

    public final boolean askYN(ScriptFunction scriptFunction, Answer yes, Answer no) {
        if (yes.meaning == Meaning.NO || no.meaning == Meaning.YES) {
            Answer swap = yes;
            yes = no;
            no = swap;
        }

        teaselib.Answer answer = showChoices(Arrays.asList(yes, no), scriptFunction);
        return answer.meaning == Meaning.YES || answer == yes;
    }

    public final void deny(String no) {
        showChoices(Arrays.asList(Answer.no(no)));
    }

    public final boolean deny(ScriptFunction scriptFunction, String no) {
        return showChoices(Arrays.asList(Answer.no(no)), scriptFunction) != Answer.Timeout;
    }

    public final boolean deny(RunnableScript script, String no) {
        return showChoices(Arrays.asList(Answer.no(no)), new ScriptFunction(script)) != Answer.Timeout;
    }

    public final boolean deny(CallableScript<Answer> script, String no) {
        return showChoices(Arrays.asList(Answer.no(no)), new ScriptFunction(script)) != Answer.Timeout;
    }

    public final void agree(String yes) {
        showChoices(Arrays.asList(Answer.yes(yes)));
    }

    public final boolean agree(ScriptFunction scriptFunction, String yes) {
        return showChoices(Arrays.asList(Answer.yes(yes)), scriptFunction) != Answer.Timeout;
    }

    public final boolean agree(RunnableScript script, String yes) {
        return showChoices(Arrays.asList(Answer.yes(yes)), new ScriptFunction(script)) != Answer.Timeout;
    }

    public final boolean agree(CallableScript<Answer> script, String yes) {
        return showChoices(Arrays.asList(Answer.yes(yes)), new ScriptFunction(script)) != Answer.Timeout;
    }

    public final void chat(String chat) {
        showChoices(Arrays.asList(Answer.resume(chat)), null, Intention.Chat);
    }

    public final String chat(ScriptFunction scriptFunction, String chat) {
        return showChoices(Arrays.asList(Answer.resume(chat)), scriptFunction, Intention.Chat).text.get(0);
    }

    public final String chat(RunnableScript script, String chat) {
        return showChoices(Arrays.asList(Answer.resume(chat)), new ScriptFunction(script), Intention.Chat).text.get(0);
    }

    public final String chat(CallableScript<Answer> script, String chat) {
        return showChoices(Arrays.asList(Answer.resume(chat)), new ScriptFunction(script), Intention.Chat).text.get(0);
    }

    public final void chat(Answer chat) {
        showChoices(Arrays.asList(chat), null, Intention.Chat);
    }

    public final boolean chat(ScriptFunction scriptFunction, Answer chat) {
        return showChoices(Arrays.asList(chat), scriptFunction, Intention.Chat) != Answer.Timeout;
    }

    public final boolean chat(RunnableScript script, Answer chat) {
        return showChoices(Arrays.asList(chat), new ScriptFunction(script), Intention.Chat) != Answer.Timeout;
    }

    public final boolean chat(CallableScript<Answer> script, Answer chat) {
        return showChoices(Arrays.asList(chat), new ScriptFunction(script), Intention.Chat) != Answer.Timeout;
    }

    /**
     * Display an array of checkboxes to set or unset
     * 
     * @param caption
     *            The caption of the checkbox area
     * @param choices
     *            The labels of the check boxes
     * @param values
     *            Indicates whether each item is set or unset by setting the corresponding index to false or true.
     * @return
     */
    public List<Boolean> showItems(String caption, List<String> choices, List<Boolean> values, boolean allowCancel) {
        completeMandatory();
        List<Boolean> results = teaseLib.host.showCheckboxes(caption, choices, values, allowCancel);
        endAll();
        return results;
    }

    public boolean showItems(String caption, Items items, boolean allowCancel) {
        List<Item> itemList = new ArrayList<>();
        List<String> choices = new ArrayList<>();
        List<Boolean> values = new ArrayList<>();
        for (Item item : items) {
            itemList.add(item);
            choices.add(item.displayName());
            values.add(item.isAvailable());
        }

        List<Boolean> results = showItems(caption, choices, values, allowCancel);
        if (results != null) {
            for (int i = 0; i < itemList.size(); i++) {
                itemList.get(i).setAvailable(results.get(i));
            }
            return true;
        }
        return false;
    }

    public State.Options apply(Item item, Message firstCommand, Answer command1stConfirmation, Message secondCommand,
            Answer command2ndConfirmation, Message intermediateInstructions, Message completionQuestion,
            Answer completionConfirmation, Answer prolongationExcuse, Message prolongationComment) {

        HumanPoseScriptInteraction poseEstimation = interaction(HumanPoseScriptInteraction.class);
        completeMandatory();

        if (poseEstimation.getPose(Status).is(Available)) {
            BooleanSupplier faceToFace = () -> poseEstimation.getPose(Proximity).is(HumanPose.Proximity.FACE2FACE);
            ScriptFunction faceToFaceInfinite = poseEstimation.autoConfirm(Proximity, HumanPose.Proximity.FACE2FACE);
            ScriptFunction notFaceToFace = poseEstimation.autoConfirm(Proximity, 5, TimeUnit.SECONDS,
                    HumanPose.Proximity.NOTFACE2FACE);
            ScriptFunction notFaceToFaceInfinite = poseEstimation.autoConfirm(Proximity,
                    HumanPose.Proximity.NOTFACE2FACE);

            append(firstCommand);
            while (true) {
                completeMandatory();
                if (faceToFace.getAsBoolean() && !chat(notFaceToFace, command1stConfirmation)) {
                    append(secondCommand);
                    completeMandatory();
                    if (faceToFace.getAsBoolean()) {
                        chat(notFaceToFaceInfinite, command2ndConfirmation);
                    }
                }

                // TODO implement instructions as infinite script function, cancel when slave is face2face
                // use supplier/stream/iterator for random or sequential messages, increase delay between messages
                show(item);
                append(Message.Delay5s);
                append(intermediateInstructions);
                completeMandatory();
                faceToFaceInfinite.call();

                say(completionQuestion);
                if (reply(completionConfirmation, prolongationExcuse).meaning != Meaning.NO) {
                    break;
                } else {
                    say(prolongationComment);
                    notFaceToFace.call();
                }
            }
        } else {
            append(firstCommand);
            chat(timeoutWithAutoConfirmation(5), command1stConfirmation);
            while (true) {
                append(Message.Delay10s);
                if (reply(() -> {
                    append(intermediateInstructions);
                    append(Message.Delay10s);
                }, completionConfirmation) == Answer.Timeout) {
                    say(completionQuestion);
                    if (reply(completionConfirmation, prolongationExcuse).meaning != Meaning.NO) {
                        break;
                    } else {
                        say(prolongationComment);
                        append(Message.Delay5to10s);
                    }
                } else {
                    break;
                }
            }
        }

        State.Options options = item.apply();
        return options;
    }

}
