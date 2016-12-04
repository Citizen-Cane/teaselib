package teaselib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.ScriptFunction.Relation;
import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;
import teaselib.core.events.Event;
import teaselib.core.events.EventSource;
import teaselib.core.media.MediaRenderer;
import teaselib.core.media.RenderBackgroundSound;
import teaselib.core.media.RenderDelay;
import teaselib.core.media.RenderDesktopItem;
import teaselib.core.media.RenderSound;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognition.TimeoutBehavior;
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.core.util.WildcardPattern;
import teaselib.util.Items;

public abstract class TeaseScript extends TeaseScriptMath implements Runnable {
    private static final Logger logger = LoggerFactory
            .getLogger(TeaseScript.class);

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
    public TeaseScript(TeaseScript script) {
        super(script, script.actor);
    }

    /**
     * Create a sub-script with a different actor
     * 
     * @param script
     *            The script to share resources with
     * @param actor
     *            If both script and actor have the same locale, the speech
     *            recognizer is shared by both scripts
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
    public TeaseScript(TeaseLib teaseLib, ResourceLoader resources, Actor actor,
            String namespace) {
        super(teaseLib, resources, actor, namespace);
    }

    /**
     * Renders the image denoted by the resource path. The image will not be
     * displayed immediately but during the next message rendering. This is
     * because if no image is specified, an image of the dominant character will
     * be used.
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
        MediaRenderer desktopItem;
        try {
            desktopItem = new RenderDesktopItem(
                    resources.unpackEnclosingFolder(path), teaseLib);
            queueRenderer(desktopItem);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public Object setBackgroundSound(String path) {
        RenderBackgroundSound audioHandle = new RenderBackgroundSound(resources,
                path, teaseLib);
        queueBackgropundRenderer(audioHandle);
        return audioHandle;
    }

    public Object setSound(String path) {
        RenderSound soundRenderer = new RenderSound(resources, path, teaseLib);
        queueRenderer(soundRenderer);
        Object audioHandle = soundRenderer;
        return audioHandle;
    }

    public void stopSound(Object audioHandle) {
        if (audioHandle instanceof MediaRenderer.Threaded) {
            ((MediaRenderer.Threaded) audioHandle).interrupt();
        } else {
            teaseLib.host.stopSound(audioHandle);
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

    public void say(String... message) {
        say(new Message(actor, message));
    }

    public void say(List<String> message) {
        say(new Message(actor, message));
    }

    public void say(Message message) {
        renderMessage(message, TextToSpeechPlayer.instance());
    }

    /**
     * Show instructional text, this is not spoken, just displayed.
     * 
     * @param message
     *            The text to be displayed, or null to display no message at all
     */
    public void show(String text) {
        final Message message;
        if (text != null) {
            message = new Message(actor, text);
        } else {
            message = new Message(actor);
        }
        show(message);
    }

    public void show(String... message) {
        show(new Message(actor, message));
    }

    public void show(Message message) {
        renderMessage(message, null);
    }

    /**
     * Show an intertitle ({@link https://en.wikipedia.org/wiki/Intertitle}) to
     * reveal information or give instructions that are encessary to bring the
     * script foraward, but aren't suitable to be given by a character.
     * <p>
     * For instance if you are to accidently spill coffee on the floor, the
     * command to do shouldn't be given by any of the actors you're interacting
     * with, as that would break their character.
     * 
     * The command works the same as the {@link #show(String ...)} command, but
     * there will be no image associated with it.
     * 
     * @param text
     *            The text to show with the intertitle.
     */
    public void showInterTitle(String... text) {
        renderIntertitle(text);
    }

    /**
     * Displays the requested choices in the user interface after the mandatory
     * parts of all renderers have been completed. This means especially that
     * all text has been displayed and spoken.
     * 
     * @param choices
     *            The prompts to be displayed in the user interface
     * @return The choice object that has been selected by the user.
     */
    public final String reply(List<String> choices) {
        return super.showChoices(null, choices);
    }

    public final String reply(Confidence confidence, List<String> choices) {
        return super.showChoices(null, confidence, choices);
    }

    /**
     * Displays the requested choices in the user interface after the mandatory
     * parts of all renderers have been completed. This means especially that
     * all text has been displayed and spoken.
     * 
     * @param choice
     *            The first prompt to be displayed by the user interface.
     * @param more
     *            More prompts to be displayed by the user interface
     * @return The choice object that has been selected by the user.
     */
    public final String reply(String choice, String... more) {
        List<String> choices = buildChoicesFromArray(choice, more);
        return reply(choices);
    }

    /**
     * Displays the requested choices in the user interface after the mandatory
     * parts of all renderers have been completed. This means especially that
     * all text has been displayed and spoken.
     * 
     * @param choices
     *            The prompts to be displayed in the user interface
     * @return The choice object that has been selected by the user, or
     *         {@link TeaseScript#Timeout} if the script function completes.
     */
    public final String reply(ScriptFunction scriptFunction,
            List<String> choices) {
        return super.showChoices(scriptFunction, choices);
    }

    /**
     * Displays the requested choices in the user interface after the mandatory
     * parts of all renderers have been completed. This means especially that
     * all text has been displayed and spoken.
     * 
     * @param choice
     *            The first prompt to be displayed by the user interface.
     * @param more
     *            More prompts to be displayed by the user interface
     * @return The choice object that has been selected by the user, or
     *         {@link TeaseScript#Timeout} if the script function completes.
     */
    public final String reply(ScriptFunction scriptFunction, String choice,
            String... more) {
        List<String> choices = buildChoicesFromArray(choice, more);
        return reply(scriptFunction, choices);
    }

    protected abstract class SpeechRecognitionAwareTimeoutScriptFunction
            extends ScriptFunction {
        final long seconds;

        boolean inDubioMitius = false;

        public SpeechRecognitionAwareTimeoutScriptFunction(long seconds,
                Relation relation) {
            super(relation);
            this.seconds = seconds;
        }

        protected void awaitTimeout(
                final SpeechRecognition.TimeoutBehavior timeoutBehavior) {
            final Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs> recognitionStarted;
            final EventSource<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs> recognitionStartedEvents = SpeechRecognizer.instance
                    .get(actor.getLocale()).events.recognitionStarted;
            if (timeoutBehavior == TimeoutBehavior.InDubioMitius) {
                // disable timeout on first speech recognition event
                // (non-audio)
                final Thread scriptFunctionThread = Thread.currentThread();
                recognitionStarted = new Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs>() {
                    @Override
                    public void run(SpeechRecognitionImplementation sender,
                            SpeechRecognitionStartedEventArgs eventArgs) {
                        logger.info("-" + scriptFunctionThread.getName()
                                + " - : Disabling timeout "
                                + timeoutBehavior.toString());
                        inDubioMitius = true;
                    }
                };
                recognitionStartedEvents.add(recognitionStarted);
            } else {
                recognitionStarted = null;
            }
            try {
                teaseLib.sleep(seconds, TimeUnit.SECONDS);
                if (timeoutBehavior != TimeoutBehavior.InDubioContraReum
                        && SpeechRecognition.isSpeechRecognitionInProgress()) {
                    logger.info("Completing speech recognition "
                            + timeoutBehavior.toString());
                    SpeechRecognition.completeSpeechRecognitionInProgress();
                }
            } finally {
                if (recognitionStarted != null) {
                    recognitionStartedEvents.remove(recognitionStarted);
                }
            }
            if (!inDubioMitius) {
                result = Timeout;
                logger.info("Script function timeout triggered");
            } else {
                logger.info("Timeout ignored " + timeoutBehavior.toString());
            }
        }
    }

    /**
     * Wait until the timeout duration has elapsed, wait for ongoing speech
     * recognition to complete, dismiss the buttons and return
     * {@link teaselib.ScriptFunction#Timeout} instead of a choice
     * 
     * The function waits until speech recognition is completed before marking
     * the choice as "Timed out", because the user has usually completed the
     * requested action before uttering a choice, and because speaking takes
     * more time than pressing a button.
     * 
     * @param seconds
     *            The timeout duration
     * @param timoutBehavior
     *            How speech recognition is handled when the timeout has been
     *            reached
     * @return A script function that accomplishes the described behavior.
     */
    public ScriptFunction timeout(long seconds,
            final SpeechRecognition.TimeoutBehavior timoutBehavior) {
        return new SpeechRecognitionAwareTimeoutScriptFunction(seconds,
                Relation.Autonomous) {
            @Override
            public void run() {
                awaitTimeout(timoutBehavior);
            }
        };
    }

    /**
     * Wait until the timeout duration has elapsed, wait for ongoing speech
     * recognition to complete, then wait until the user makes a choice and
     * return {@link teaselib.ScriptFunction#Timeout} instead of the users'
     * choice.
     * <p>
     * While this behavior can be implemented with a standard button, the
     * function waits until speech recognition is completed before marking the
     * choice as "Timed out", because the user has usually completed the
     * requested action before uttering a choice, and because speaking takes
     * more time than pressing a button.
     * 
     * @param seconds
     *            The timeout duration
     * @param timoutBehavior
     *            How speech recognition is handled when the timeout has been
     *            reached
     * @return A script function that accomplishes the described behavior.
     */
    public ScriptFunction timeoutWithConfirmation(long seconds,
            final SpeechRecognition.TimeoutBehavior timoutBehavior) {
        return new SpeechRecognitionAwareTimeoutScriptFunction(seconds,
                Relation.Confirmation) {
            @Override
            public void run() {
                awaitTimeout(timoutBehavior);
                sleep(Infinite, TimeUnit.SECONDS);
            }
        };
    }

    /**
     * Wait until the timeout duration has elapsed, and wait for ongoing speech
     * recognition to complete. If the duration elapses, return
     * {@link teaselib.ScriptFunction#Timeout} instead of waiting for user
     * input.
     * <p>
     * 
     * The function behaves like a normal prompt, but the buttons are
     * automatically dismissed when the duration expires.
     * 
     * @param seconds
     *            The timeout duration
     * @param timoutBehavior
     *            How speech recognition is handled when the timeout has been
     *            reached
     * @return A script function that accomplishes the described behavior.
     */
    public ScriptFunction timeoutWithAutoConfirmation(long seconds,
            final SpeechRecognition.TimeoutBehavior timoutBehavior) {
        return new SpeechRecognitionAwareTimeoutScriptFunction(seconds,
                Relation.Confirmation) {
            @Override
            public void run() {
                awaitTimeout(timoutBehavior);
            }
        };
    }

    /**
     * Displays the requested choices in the user interface after the mandatory
     * parts of all renderers have been completed. This means especially that
     * all text has been displayed and spoken.
     * 
     * @param choice
     *            The first prompt to be displayed by the user interface.
     * @param more
     *            More prompts to be displayed by the user interface
     * @return The index of the choice object in the argument list that has been
     *         selected by the user.
     */
    public final int replyIndex(String choice, String... more) {
        List<String> choices = buildChoicesFromArray(choice, more);
        return replyIndex(choices);
    }

    /**
     * Displays the requested choices in the user interface after the mandatory
     * parts of all renderers have been completed. This means especially that
     * all text has been displayed and spoken.
     * 
     * @param choices
     *            The prompts to be displayed in the user interface
     * @return The index of the choice object in the {@code choices} list that
     *         has been selected by the user.
     */
    public final int replyIndex(List<String> choices) {
        String answer = reply(choices);
        return choices.indexOf(answer);
    }

    /**
     * Displays the requested choices in the user interface after the mandatory
     * parts of all renderers have been completed. This means especially that
     * all text has been displayed and spoken.
     * 
     * @param yes
     *            The first prompt.
     * @param no
     *            The second prompt.
     * @return True if {@code yes} has been selected, false if {@code no} has
     *         been selected.
     */
    public final boolean askYN(String yes, String no) {
        return reply(yes, no) == yes;
    }

    /**
     * If the reply should be non-blocking, e.g. not interrupt the flow of the
     * script, the recognition accuracy might be lowered to become less picky.
     * 
     * Best used for replies that can be easily dismissed without consequences,
     * like in conversations.
     * 
     * Or in situations that involve timing, where a quick reply is necessary,
     * but the actual words don't matter.
     * 
     * @param choice
     *            The prompt to be displayed by the user interface.
     * @param recognitionConfidence
     *            The confidence threshold used for speech recognition.
     * @return The choice object that has been selected by the user.
     */
    public final String reply(String choice, Confidence recognitionConfidence) {
        List<String> choices = buildChoicesFromArray(choice);
        return showChoices(null, recognitionConfidence, choices);
    }

    /**
     * Displays the requested choices in the user interface after the mandatory
     * parts of all renderers have been completed. This means especially that
     * all text has been displayed and spoken.
     * 
     * @param recognitionConfidence
     *            The confidence threshold used for speech recognition.
     * @param choice
     *            The first prompt to be displayed by the user interface.
     * @param more
     *            More prompts to be displayed by the user interface
     * 
     * @return The choice object that has been selected by the user, or
     *         {@link TeaseScript#Timeout} if the script function completes.
     */
    public final String reply(ScriptFunction scriptFunction,
            Confidence recognitionConfidence, List<String> choices) {
        return showChoices(scriptFunction, recognitionConfidence, choices);
    }

    /**
     * Display an array of checkboxes to set or unset
     * 
     * @param caption
     *            The caption of the checkbox area
     * @param choices
     *            The labels of the check boxes
     * @param values
     *            Indicates whether each item is set or unset by setting the
     *            corresponding index to false or true.
     * @return
     */
    public List<Boolean> showItems(String caption, List<String> choices,
            List<Boolean> values, boolean allowCancel) {
        completeMandatory();
        List<Boolean> results = teaseLib.host.showCheckboxes(caption, choices,
                values, allowCancel);
        endAll();
        return results;
    }

    public boolean showItems(String caption, Items<?> items,
            boolean allowCancel) {
        List<String> choices = new ArrayList<String>(items.size());
        List<Boolean> values = new ArrayList<Boolean>(items.size());
        for (int i = 0; i < items.size(); i++) {
            choices.add(items.get(i).displayName);
            values.add(items.get(i).isAvailable());
        }
        List<Boolean> results = showItems(caption, choices, values,
                allowCancel);
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
     *            The wildcard pattern ("?" replaces a single, "*" multiple
     *            characters).
     * @return A list of resources that matches the wildcard pattern.
     */
    public List<String> resources(String wildcardPattern) {
        Pattern pattern = WildcardPattern.compile(
                resources.getClassLoaderAbsoluteResourcePath(wildcardPattern));
        Collection<String> items = resources.resources(pattern);
        final int size = items.size();
        if (size > 0) {
            logger.info(getClass().getSimpleName() + ": '" + wildcardPattern
                    + "' yields " + size + " resources");
        } else {
            logger.info(getClass().getSimpleName() + ": '" + wildcardPattern
                    + "' doesn't yield any resources");
        }
        return new ArrayList<String>(items);
    }

}
