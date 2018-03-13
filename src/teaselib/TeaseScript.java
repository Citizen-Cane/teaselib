package teaselib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.ScriptFunction.Relation;
import teaselib.core.ResourceLoader;
import teaselib.core.Script;
import teaselib.core.TeaseLib;
import teaselib.core.events.Event;
import teaselib.core.events.EventSource;
import teaselib.core.media.MediaRenderer;
import teaselib.core.media.RenderDelay;
import teaselib.core.media.RenderDesktopItem;
import teaselib.core.media.RenderSound;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognition.TimeoutBehavior;
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.speechrecognition.events.SpeechRecognizedEventArgs;
import teaselib.core.util.WildcardPattern;
import teaselib.functional.CallableScript;
import teaselib.functional.RunnableScript;
import teaselib.util.Items;

public abstract class TeaseScript extends TeaseScriptMath {
    private static final Logger logger = LoggerFactory.getLogger(TeaseScript.class);

    /**
     * see {@link teaselib.ScriptFunction#Timeout}
     */
    public static final String Timeout = ScriptFunction.Timeout;

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
                MediaRenderer desktopItem = new RenderDesktopItem(resources.unpackEnclosingFolder(path), teaseLib);
                queueRenderer(desktopItem);
            } catch (IOException e) {
                if (Boolean.parseBoolean(teaseLib.config.get(Config.Debug.StopOnRenderError))) {
                    throw new RuntimeException(e);
                } else {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    public void setBackgroundSound(String path) {
        if (Boolean.parseBoolean(teaseLib.config.get(Config.Render.Sound))) {
            queueBackgropundRenderer(new RenderSound(resources, path, teaseLib));
        }
    }

    public void setSound(String path) {
        if (Boolean.parseBoolean(teaseLib.config.get(Config.Render.Sound))) {
            queueRenderer(new RenderSound(resources, path, teaseLib));
        }
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
        queueRenderer(new RenderDelay(seconds, teaseLib));
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
        return showChoices(Answer.all(text), null);
    }

    public final String reply(Confidence confidence, List<String> text) {
        return showChoices(Answer.all(text), null, confidence);
    }

    /**
     * Displays the requested choices in the user interface after the mandatory parts of all renderers have been
     * completed. This means especially that all text has been displayed and spoken.
     * 
     * @param text
     *            The first prompt to be displayed by the user interface.
     * @param more
     *            More prompts to be displayed by the user interface
     * @return The choice object that has been selected by the user.
     */
    public final String reply(String text, String... more) {
        List<String> choices = buildChoicesFromArray(text, more);
        return reply(choices);
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
        return showChoices(Answer.all(text), scriptFunction);
    }

    public final String reply(RunnableScript script, List<String> text) {
        return showChoices(Answer.all(text), new ScriptFunction(script));
    }

    public final String reply(CallableScript<String> script, List<String> text) {
        return showChoices(Answer.all(text), new ScriptFunction(script));
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
        List<String> choices = buildChoicesFromArray(text, more);
        return reply(scriptFunction, choices);
    }

    public final String reply(RunnableScript script, String text, String... more) {
        List<String> choices = buildChoicesFromArray(text, more);
        return reply(new ScriptFunction(script), choices);
    }

    public final String reply(CallableScript<String> script, String text, String... more) {
        List<String> choices = buildChoicesFromArray(text, more);
        return reply(new ScriptFunction(script), choices);
    }

    protected String awaitTimeout(long seconds, SpeechRecognition.TimeoutBehavior timeoutBehavior) {
        AtomicBoolean ignoreTimeoutInDubioMitius = new AtomicBoolean(false);

        Event<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> recognitionRejected;
        SpeechRecognition speechRecognizer = teaseLib.globals.get(SpeechRecognizer.class).get(actor.locale());
        EventSource<SpeechRecognitionImplementation, SpeechRecognizedEventArgs> speechDetectedEvents = speechRecognizer.events.recognitionRejected;
        if (timeoutBehavior == TimeoutBehavior.InDubioMitius) {
            Thread scriptFunctionThread = Thread.currentThread();
            // TODO Should be SpeechDetectedEvent
            recognitionRejected = (sender, eventArgs) -> {
                if (!ignoreTimeoutInDubioMitius.get()) {
                    logger.info("-" + scriptFunctionThread.getName() + " - : timeout disabled " + timeoutBehavior);
                    ignoreTimeoutInDubioMitius.set(true);
                }
            };
            speechDetectedEvents.add(recognitionRejected);
        } else {
            recognitionRejected = null;
        }
        try {
            teaseLib.sleep(seconds, TimeUnit.SECONDS);
            if (timeoutBehavior != TimeoutBehavior.InDubioContraReum
                    && speechRecognizer.isSpeechRecognitionInProgress()) {
                logger.info("Completing speech recognition " + timeoutBehavior);
                SpeechRecognition.completeSpeechRecognitionInProgress();
            }
        } finally {
            if (recognitionRejected != null) {
                speechDetectedEvents.remove(recognitionRejected);
            }
        }
        String result;
        if (ignoreTimeoutInDubioMitius.get()) {
            logger.info(/* relation + */ " timeout ignored " + timeoutBehavior);
            result = null;
        } else {
            logger.info("Script function confirm timeout");
            result = Timeout;
        }

        return result;
    }

    /**
     * Wait until the timeout duration has elapsed, wait for ongoing speech recognition to complete, dismiss the buttons
     * and return {@link teaselib.ScriptFunction#Timeout} instead of a choice
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
    public ScriptFunction timeout(long seconds, final SpeechRecognition.TimeoutBehavior timeoutBehavior) {
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
    public ScriptFunction timeoutWithConfirmation(long seconds, SpeechRecognition.TimeoutBehavior timeoutBehavior) {
        return new ScriptFunction(() -> {
            String result = awaitTimeout(seconds, timeoutBehavior);
            sleep(ScriptFunction.Infinite, TimeUnit.SECONDS);
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
    public ScriptFunction timeoutWithAutoConfirmation(long seconds, SpeechRecognition.TimeoutBehavior timeoutBehavior) {
        return new ScriptFunction(() -> awaitTimeout(seconds, timeoutBehavior), Relation.Confirmation);
    }

    /**
     * Displays the requested choices in the user interface after the mandatory parts of all renderers have been
     * completed. This means especially that all text has been displayed and spoken.
     * 
     * @param choice
     *            The first prompt to be displayed by the user interface.
     * @param more
     *            More prompts to be displayed by the user interface
     * @return The index of the choice object in the argument list that has been selected by the user.
     */
    public final int replyIndex(String choice, String... more) {
        List<String> choices = buildChoicesFromArray(choice, more);
        return replyIndex(choices);
    }

    /**
     * Displays the requested choices in the user interface after the mandatory parts of all renderers have been
     * completed. This means especially that all text has been displayed and spoken.
     * 
     * @param choices
     *            The prompts to be displayed in the user interface
     * @return The index of the choice object in the {@code choices} list that has been selected by the user.
     */
    public final int replyIndex(List<String> choices) {
        String answer = reply(choices);
        return choices.indexOf(answer);
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
        return showChoices(Arrays.asList(Answer.yes(yes), Answer.no(no)), null) == yes;
    }

    public final boolean askYN(ScriptFunction scriptFunction, String yes, String no) {
        return showChoices(Arrays.asList(Answer.yes(yes), Answer.no(no)), scriptFunction) == yes;
    }

    public final boolean askYN(RunnableScript script, String yes, String no) {
        return showChoices(Arrays.asList(Answer.yes(yes), Answer.no(no)), new ScriptFunction(script)) == yes;
    }

    public final void deny(String no) {
        showChoices(Arrays.asList(Answer.no(no)));
    }

    public final void deny(ScriptFunction scriptFunction, String no) {
        showChoices(Arrays.asList(Answer.no(no)), scriptFunction);
    }

    public final void deny(RunnableScript script, String no) {
        showChoices(Arrays.asList(Answer.no(no)), new ScriptFunction(script));
    }

    public final void agree(String yes) {
        showChoices(Arrays.asList(Answer.yes(yes)));
    }

    public final void agree(ScriptFunction scriptFunction, String yes) {
        showChoices(Arrays.asList(Answer.yes(yes)), scriptFunction);
    }

    public final void agree(RunnableScript script, String yes) {
        showChoices(Arrays.asList(Answer.yes(yes)), new ScriptFunction(script));
    }

    /**
     * If the reply should be non-blocking, e.g. not interrupt the flow of the script, the recognition accuracy might be
     * lowered to become less picky.
     * 
     * Best used for replies that can be easily dismissed without consequences, like in conversations.
     * 
     * Or in situations that involve timing, where a quick reply is necessary, but the actual words don't matter.
     * 
     * @param recognitionConfidence
     *            The confidence threshold used for speech recognition.
     * @param choice
     *            The prompt to be displayed by the user interface.
     * 
     * @return The choice object that has been selected by the user.
     */
    public final String reply(Confidence recognitionConfidence, String text) {
        return showChoices(Answer.all(text), null, recognitionConfidence);
    }

    /**
     * Displays the requested choices in the user interface after the mandatory parts of all renderers have been
     * completed. This means especially that all text has been displayed and spoken.
     * 
     * @param recognitionConfidence
     *            The confidence threshold used for speech recognition.
     * @param text
     *            The first prompt to be displayed by the user interface.
     * @param more
     *            More prompts to be displayed by the user interface
     * 
     * @return The choice object that has been selected by the user, or {@link TeaseScript#Timeout} if the script
     *         function completes.
     */
    public final String reply(ScriptFunction scriptFunction, Confidence recognitionConfidence, List<String> text) {
        return showChoices(Answer.all(text), scriptFunction, recognitionConfidence);
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
        List<String> choices = new ArrayList<>(items.size());
        List<Boolean> values = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            choices.add(items.get(i).displayName());
            values.add(items.get(i).isAvailable());
        }
        List<Boolean> results = showItems(caption, choices, values, allowCancel);
        if (results != null) {
            for (int i = 0; i < items.size(); i++) {
                items.get(i).setAvailable(results.get(i));
            }
            return true;
        }
        return false;
    }

    /**
     * Build a list of resource strings from a {@link WildcardPattern}.
     * 
     * @param wildcardPattern
     *            The wildcard pattern ("?" replaces a single, "*" multiple characters).
     * @return A list of resources that matches the wildcard pattern.
     */
    public List<String> resources(String wildcardPattern) {
        Pattern pattern = WildcardPattern.compile(resources.getClassLoaderAbsoluteResourcePath(wildcardPattern));
        Collection<String> items = resources.resources(pattern);
        final int size = items.size();
        if (size > 0) {
            logger.info(getClass().getSimpleName() + ": '" + wildcardPattern + "' yields " + size + " resources");
        } else {
            logger.info(getClass().getSimpleName() + ": '" + wildcardPattern + "' doesn't yield any resources");
        }
        return new ArrayList<>(items);
    }

}
