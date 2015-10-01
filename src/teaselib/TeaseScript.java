package teaselib;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import teaselib.ScriptFunction.Relation;
import teaselib.core.MediaRenderer;
import teaselib.core.RenderBackgroundSound;
import teaselib.core.RenderDelay;
import teaselib.core.RenderDesktopItem;
import teaselib.core.RenderSound;
import teaselib.core.ResourceLoader;
import teaselib.core.events.Event;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognition.TimeoutBehavior;
import teaselib.core.speechrecognition.SpeechRecognitionImplementation;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.speechrecognition.events.SpeechRecognitionStartedEventArgs;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.util.Items;

public abstract class TeaseScript extends TeaseScriptMath implements Runnable {

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
     * Create a new script
     * 
     * @param teaseLib
     * @param locale
     * @param namespace
     */
    public TeaseScript(TeaseLib teaseLib, ResourceLoader resources,
            String locale, String namespace) {
        this(teaseLib, resources, new Actor("Dominant", locale), namespace);
    }

    /**
     * Create a new script
     * 
     * @param teaseLib
     * @param actor
     * @param namespace
     */
    public TeaseScript(TeaseLib teaseLib, ResourceLoader resources,
            Actor actor, String namespace) {
        super(teaseLib, resources, actor, namespace);
    }

    String resource(String path) {
        String folder = getClass().getPackage().getName().replace(".", "/")
                + "/";
        boolean isRelative = !path.startsWith(folder);
        if (isRelative) {
            return folder + path;
        } else {
            return path;
        }
    }

    /**
     * Renders the image denoted by the path. The image will not be displayed
     * immediately but during the next message rendering. This is because if no
     * image is specified, an image of the dominant character will be used.
     * 
     * @param path
     *            The path to the image
     */
    public void setImage(String path) {
        if (path == null) {
            displayImage = Message.NoImage;
        } else if (path.equalsIgnoreCase(Message.DominantImage)) {
            displayImage = Message.DominantImage;
        } else if (path.equalsIgnoreCase(Message.NoImage)) {
            displayImage = Message.NoImage;
        } else {
            displayImage = path;
        }
    }

    public void showDesktopItem(String path) {
        URL url = resources.url(path);
        if (url != null) {
            URI uri = null;
            try {
                uri = url.toURI();
            } catch (URISyntaxException e) {
                TeaseLib.log(this, e);
            }
            MediaRenderer desktopItem = new RenderDesktopItem(uri);
            queueRenderer(desktopItem);
        }
    }

    public void setSound(String path) {
        queueRenderer(new RenderSound(resources, path));
    }

    /**
     * Play a sound in the background. The sound starts when the next message is
     * displayed, does not cause the script to wait for its completion. To stop
     * the sound, stopBackgroundSound() can be called.
     * 
     * @param path
     */
    public Object playBackgroundSound(String path) {
        RenderBackgroundSound renderBackgroundSound = new RenderBackgroundSound(
                resources, path);
        queueRenderer(renderBackgroundSound);
        return renderBackgroundSound;
    }

    public void stopBackgroundSound(Object handle) {
        teaseLib.host.stopSound(handle);
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
        queueRenderer(new RenderDelay(seconds));
    }

    public void say(String text) {
        Message message = new Message(actor);
        if (text != null) {
            message.add(text);
        }
        say(message);
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
     *            The text to be displayed
     */
    public void show(String text) {
        show(new Message(actor, text));
    }

    public void show(String... message) {
        show(new Message(actor, message));
    }

    public void show(Message message) {
        renderMessage(message, null);
    }

    /**
     * Displays the requested choices in the user interface after the mandatory
     * parts of all renderers have been completed. This means especially that
     * all text has been displayed and spoken.
     * 
     * @param choices
     * @return
     */
    public String reply(final List<String> choices) {
        return showChoices(null, choices);
    }

    public String reply(String choice, String... more) {
        List<String> choices = buildChoicesFromArray(choice, more);
        return reply(choices);
    }

    /**
     * @param scriptFunction
     * @param choices
     * @return
     */
    public String reply(ScriptFunction scriptFunction,
            final List<String> choices) {
        String chosen = showChoices(scriptFunction, choices);
        return chosen;
    }

    public String reply(ScriptFunction scriptFunction, String choice,
            String... more) {
        List<String> choices = buildChoicesFromArray(choice, more);
        return reply(scriptFunction, choices);
    }

    protected abstract class SpeechRecognitionAwareTimeoutScriptFunction extends
            ScriptFunction {
        final long seconds;
        final SpeechRecognition.TimeoutBehavior timeoutBehavior;

        boolean inDubioMitius = false;

        public SpeechRecognitionAwareTimeoutScriptFunction(final long seconds,
                final SpeechRecognition.TimeoutBehavior timoutBehavior,
                Relation relation) {
            super(relation);
            this.seconds = seconds;
            this.timeoutBehavior = timoutBehavior;
        }

        protected void waitAndJudge() {
            final Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs> recognitionStarted;
            if (timeoutBehavior == TimeoutBehavior.InDubioMitius) {
                // disable timeout on first speech recognition event
                // (non-audio)
                final Thread scriptFunctionThread = Thread.currentThread();
                recognitionStarted = new Event<SpeechRecognitionImplementation, SpeechRecognitionStartedEventArgs>() {
                    @Override
                    public void run(SpeechRecognitionImplementation sender,
                            SpeechRecognitionStartedEventArgs eventArgs) {
                        TeaseLib.log("-" + scriptFunctionThread.getName()
                                + " - : Disabling timeout "
                                + timeoutBehavior.toString().toLowerCase());
                        inDubioMitius = true;
                    }
                };
                SpeechRecognizer.instance.get(actor.locale).events.recognitionStarted
                        .add(recognitionStarted);
            } else {
                recognitionStarted = null;
            }
            try {
                teaseLib.sleep(seconds, TimeUnit.SECONDS);
                if (timeoutBehavior != TimeoutBehavior.InDubioContraReum
                        && SpeechRecognition.isSpeechRecognitionInProgress()) {
                    TeaseLib.log("Completing speech recognition "
                            + timeoutBehavior.toString().toLowerCase());
                    SpeechRecognition.completeSpeechRecognitionInProgress();
                }
            } finally {
                if (recognitionStarted != null) {
                    SpeechRecognizer.instance.get(actor.locale).events.recognitionStarted
                            .remove(recognitionStarted);
                }
            }
            if (!inDubioMitius) {
                result = Timeout;
                TeaseLib.log("Script function timeout triggered");
            } else {
                TeaseLib.log("Timeout ignored "
                        + timeoutBehavior.toString().toLowerCase());
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
     * @return A script function that accomplishes the described behavior.
     */
    public ScriptFunction timeout(final long seconds,
            final SpeechRecognition.TimeoutBehavior timoutBehavior) {
        return new SpeechRecognitionAwareTimeoutScriptFunction(seconds,
                timoutBehavior, Relation.Autonomous) {
            @Override
            public void run() {
                waitAndJudge();
            }
        };
    }

    /**
     * Wait until the timeout duration has elapsed, wait for ongoing speech
     * recognition to complete, then wait until the user makes a choice and
     * return {@link teaselib.ScriptFunction#Timeout} instead of the users'
     * choice.
     * 
     * While this behavior can be implemented with a standard button, the
     * function waits until speech recognition is completed before marking the
     * choice as "Timed out", because the user has usually completed the
     * requested action before uttering a choice, and because speaking takes
     * more time than pressing a button.
     * 
     * @param seconds
     *            The timeout duration
     * @return A script function that accomplishes the described behavior.
     */
    public ScriptFunction timeoutWithConfirmation(final long seconds,
            final SpeechRecognition.TimeoutBehavior timoutBehavior) {
        // // Need to complete mandatories beforehand, because a timed button
        // with
        // // confirmation should appear as a normal button, instead of when the
        // // message starts displaying
        // completeMandatory();
        return new SpeechRecognitionAwareTimeoutScriptFunction(seconds,
                timoutBehavior, Relation.Confirmation) {
            @Override
            public void run() {
                waitAndJudge();
                sleep(Infinite, TimeUnit.SECONDS);
            }
        };
    }

    public int replyIndex(String choice, String... more) {
        List<String> choices = buildChoicesFromArray(choice, more);
        return replyIndex(choices);
    }

    public int replyIndex(List<String> choices) {
        String answer = reply(choices);
        return choices.indexOf(answer);
    }

    public boolean askYN(String yes, String no) {
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
     * @param recognitionConfidence
     * @return
     */
    public String reply(String choice, Confidence recognitionConfidence) {
        List<String> choices = buildChoicesFromArray(choice);
        return showChoices(null, choices, recognitionConfidence);
    }

    public String reply(ScriptFunction scriptFunction, String choice,
            Confidence recognitionConfidence) {
        List<String> choices = buildChoicesFromArray(choice);
        return showChoices(scriptFunction, choices, recognitionConfidence);
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

    public boolean showItems(String caption, Items<?> items, boolean allowCancel) {
        List<String> choices = new ArrayList<String>(items.size());
        List<Boolean> values = new ArrayList<Boolean>(items.size());
        for (int i = 0; i < items.size(); i++) {
            choices.add(items.get(i).displayName);
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
}
