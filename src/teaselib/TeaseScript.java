package teaselib;

import static java.util.concurrent.TimeUnit.SECONDS;
import static teaselib.core.ai.perception.HumanPose.Interest.Proximity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.ScriptFunction.AnswerOverride;
import teaselib.ScriptFunction.Relation;
import teaselib.core.ResourceLoader;
import teaselib.core.Script;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.TeaseLib;
import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPoseScriptInteraction;
import teaselib.core.devices.release.KeyReleaseSetup;
import teaselib.core.events.Event;
import teaselib.core.events.EventSource;
import teaselib.core.media.MediaRenderer;
import teaselib.core.media.RenderDelay;
import teaselib.core.media.RenderDesktopItem;
import teaselib.core.media.RenderSound;
import teaselib.core.speechrecognition.SpeechRecognitionInputMethod;
import teaselib.core.speechrecognition.TimeoutBehavior;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.ui.InputMethods;
import teaselib.core.ui.Intention;
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
                handleAssetNotFound(e);
            }
        }
    }

    public void setBackgroundSound(String path) {
        if (Boolean.parseBoolean(teaseLib.config.get(Config.Render.Sound))) {
            try {
                scriptRenderer.queueBackgroundRenderer(new RenderSound(resources, path, teaseLib));
            } catch (IOException e) {
                handleAssetNotFound(e);
            }
        }
    }

    public void setSound(String path) {
        if (Boolean.parseBoolean(teaseLib.config.get(Config.Render.Sound))) {
            try {
                scriptRenderer.queueRenderer(new RenderSound(resources, path, teaseLib));
            } catch (IOException e) {
                handleAssetNotFound(e);
            }
        }
    }

    public MediaRenderer.Threaded getSound(String path) {
        if (Boolean.parseBoolean(teaseLib.config.get(Config.Render.Sound))) {
            try {
                return new RenderSound(resources, path, teaseLib);
            } catch (IOException e) {
                handleAssetNotFound(e);
            }
        }
        return new RenderDelay(0, teaseLib);
    }

    private void handleAssetNotFound(IOException e) {
        ExceptionUtil.handleAssetNotFound(e, teaseLib.config, logger);
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
     * @param paragraphs
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
     * @param name
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

    protected Answer awaitTimeout(long seconds, TimeoutBehavior timeoutBehavior) {
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
            sleep(seconds, TimeUnit.SECONDS);
            if (timeoutBehavior != TimeoutBehavior.InDubioContraReum && scriptRenderer.audioSync.inProgress()) {
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

    public ScriptFunction timeout(long seconds, TimeoutBehavior timeoutBehavior) {
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

    public ScriptFunction timeoutWithConfirmation(long seconds, TimeoutBehavior timeoutBehavior) {
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

    public ScriptFunction timeoutWithAutoConfirmation(long seconds, TimeoutBehavior timeoutBehavior) {
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

    public final boolean askYN(Answer.Yes yes, Answer answer) {
        return askYN(yes, Answer.no(answer.text));
    }

    public final boolean askYN(Answer answer, Answer.Yes yes) {
        return askYN(yes, Answer.no(answer.text));
    }

    public final boolean askYN(Answer answer, Answer.No no) {
        return askYN(Answer.yes(answer.text), no);
    }

    public final boolean askYN(Answer.No no, Answer answer) {
        return askYN(Answer.yes(answer.text), no);
    }

    public final boolean askYN(Answer.No no, Answer.Yes yes) {
        return askYN(yes, no);
    }

    public final boolean askYN(Answer.Yes yes, Answer.No no) {
        return askYN((ScriptFunction) null, yes, no);
    }

    public final boolean askYN(RunnableScript script, Answer.Yes yes, Answer.No no) {
        return askYN(new ScriptFunction(script), yes, no);
    }

    public final boolean askYN(ScriptFunction scriptFunction, Answer.Yes yes, Answer.No no) {
        var answer = showChoices(Arrays.asList(yes, no), scriptFunction);
        return answer == yes;
    }

    public final void deny(String... no) {
        deny(Answer.no(no));
    }

    public final void deny(Answer.No no) {
        showChoices(Arrays.asList(no));
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

    public final void deny(ScriptFunction scriptFunction, Answer.No no) {
        showChoices(Arrays.asList(no), scriptFunction);
    }

    public final void deny(RunnableScript script, Answer.No no) {
        showChoices(Arrays.asList(no), new ScriptFunction(script));
    }

    public final void deny(CallableScript<Answer> script, Answer.No no) {
        showChoices(Arrays.asList(no), new ScriptFunction(script));
    }

    public final void agree(String... yes) {
        showChoices(Arrays.asList(Answer.yes(yes)));
    }

    public final void agree(Answer.Yes yes) {
        showChoices(Arrays.asList(yes));
    }

    public final boolean agree(ScriptFunction scriptFunction, String yes) {
        return agree(scriptFunction, Answer.yes(yes));
    }

    public final boolean agree(RunnableScript script, String yes) {
        return agree(script, Answer.yes(yes));
    }

    public final boolean agree(CallableScript<Answer> script, String yes) {
        return agree(script, Answer.yes(yes));
    }

    public final boolean agree(ScriptFunction scriptFunction, Answer.Yes yes) {
        return showChoices(Arrays.asList(yes), scriptFunction) != Answer.Timeout;
    }

    public final boolean agree(RunnableScript script, Answer.Yes yes) {
        return showChoices(Arrays.asList(yes), new ScriptFunction(script)) != Answer.Timeout;
    }

    public final boolean agree(CallableScript<Answer> script, Answer.Yes yes) {
        return showChoices(Arrays.asList(yes), new ScriptFunction(script)) != Answer.Timeout;
    }

    public final void chat(String... chat) {
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
        awaitMandatoryCompleted();
        try {
            return teaseLib.host.showItems(caption, choices, values, allowCancel);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        } finally {
            endAll();
        }
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

    /**
     * Fetch items, but don't apply them (yet)
     */
    public void fetch(Item item, Message firstCommand, Answer command1stConfirmation, Message secondCommand,
            Answer command2ndConfirmation, Message progressInstructions, Message completionQuestion,
            Answer completionConfirmation, Answer prolongationExcuse) {
        fetch(new Items(item), firstCommand, command1stConfirmation, progressInstructions, completionQuestion,
                completionConfirmation, prolongationExcuse);
    }

    public void fetch(Item item, Message firstCommand, Answer command1stConfirmation, Message secondCommand,
            Answer command2ndConfirmation, Message progressInstructions, Message completionQuestion,
            Answer completionConfirmation, Answer prolongationExcuse, Message prolongationComment) {
        fetch(new Items(item), firstCommand, command1stConfirmation, progressInstructions, completionQuestion,
                completionConfirmation, prolongationExcuse, prolongationComment);
    }

    public void fetch(Items items, Message firstCommand, Answer command1stConfirmation, Message progressInstructions,
            Message completionQuestion, Answer completionConfirmation, Answer prolongationExcuse) {
        fetch(items, firstCommand, command1stConfirmation, progressInstructions, completionQuestion,
                completionConfirmation, prolongationExcuse, firstCommand);
    }

    public void fetch(Items items, Message firstCommand, Answer command1stConfirmation, Message progressInstructions,
            Message completionQuestion, Answer completionConfirmation, Answer prolongationExcuse,
            Message prolongationComment) {
        show(items);
        perform(items, firstCommand, command1stConfirmation, progressInstructions, completionQuestion,
                completionConfirmation, prolongationExcuse, prolongationComment);
    }

    /**
     * Apply items at the end of the call
     */
    public final State.Options apply(Item item, Message firstCommand, Answer command1stConfirmation,
            Message progressInstructions, Message completionQuestion, Answer completionConfirmation,
            Answer prolongationExcuse) {
        return apply(new Items(item), firstCommand, command1stConfirmation, progressInstructions, completionQuestion,
                completionConfirmation, prolongationExcuse, firstCommand);
    }

    public final State.Options apply(Item item, Message firstCommand, Answer command1stConfirmation,
            Message progressInstructions, Message completionQuestion, Answer completionConfirmation,
            Answer prolongationExcuse, Message prolongationComment) {
        return apply(new Items(item), firstCommand, command1stConfirmation, progressInstructions, completionQuestion,
                completionConfirmation, prolongationExcuse, prolongationComment);
    }

    public State.Options apply(Items items, Message firstCommand, Answer command1stConfirmation,
            Message progressInstructions, Message completionQuestion, Answer completionConfirmation,
            Answer prolongationExcuse) {
        return apply(items, firstCommand, command1stConfirmation, progressInstructions, completionQuestion,
                completionConfirmation, prolongationExcuse, firstCommand);
    }

    public State.Options apply(Items items, Message firstCommand, Answer command1stConfirmation,
            Message progressInstructions, Message completionQuestion, Answer completionConfirmation,
            Answer prolongationExcuse, Message prolongationComment) {

        KeyReleaseSetup keyRelease = interaction(KeyReleaseSetup.class);
        if (keyRelease != null) {
            if (!keyRelease.isPrepared(items) && keyRelease.canPrepare(items)) {
                keyRelease.prepare(items, all -> {
                });
            } else {
                show(items);
            }
        } else {
            show(items);
        }

        perform(items, firstCommand, command1stConfirmation, progressInstructions, completionQuestion,
                completionConfirmation, prolongationExcuse, prolongationComment);

        return items.apply();
    }

    /**
     * Remove the items at the start of the call
     */
    public final void remove(Item item, Message firstCommand, Answer command1stConfirmation,
            Message progressInstructions, Message completionQuestion, Answer completionConfirmation,
            Answer prolongationExcuse) {
        remove(item, firstCommand, command1stConfirmation, progressInstructions, completionQuestion,
                completionConfirmation, prolongationExcuse, firstCommand);
    }

    public final void remove(Item item, Message firstCommand, Answer command1stConfirmation,
            Message progressInstructions, Message completionQuestion, Answer completionConfirmation,
            Answer prolongationExcuse, Message prolongationComment) {
        remove(new Items(item), firstCommand, command1stConfirmation, progressInstructions, completionQuestion,
                completionConfirmation, prolongationExcuse, prolongationComment);
    }

    public final void remove(Items items, Message firstCommand, Answer command1stConfirmation,
            Message progressInstructions, Message completionQuestion, Answer completionConfirmation,
            Answer prolongationExcuse) {
        remove(items, firstCommand, command1stConfirmation, progressInstructions, completionQuestion,
                completionConfirmation, prolongationExcuse, firstCommand);
    }

    public void remove(Items items, Message firstCommand, Answer command1stConfirmation, Message progressInstructions,
            Message completionQuestion, Answer completionConfirmation, Answer prolongationExcuse,
            Message prolongationComment) {

        items.remove();
        perform(items, firstCommand, command1stConfirmation, progressInstructions, completionQuestion,
                completionConfirmation, prolongationExcuse, prolongationComment);
    }

    protected void perform(Items items, Message firstCommand, Answer command1stConfirmation,
            Message progressInstructions, Message completionQuestion, Answer completionConfirmation,
            Answer prolongationExcuse, Message prolongationComment) {
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
            say(firstCommand);
            awaitMandatoryCompleted();

            boolean explainAll;
            if (faceToFace.getAsBoolean()) {
                if (chat(untilNotFaceToFace, command1stConfirmation)) {
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
            say(firstCommand);
            chat(timeoutWithAutoConfirmation(5), command1stConfirmation);
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
