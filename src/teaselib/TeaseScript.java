package teaselib;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import teaselib.core.MediaRenderer;
import teaselib.core.RenderBackgroundSound;
import teaselib.core.RenderDelay;
import teaselib.core.RenderDesktopItem;
import teaselib.core.RenderSound;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.util.Item;

public abstract class TeaseScript extends TeaseScriptMath implements Runnable {

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
    public TeaseScript(TeaseLib teaseLib, String locale, String namespace) {
        this(teaseLib, new Actor("Dominant", locale), namespace);
    }

    /**
     * Create a new script
     * 
     * @param teaseLib
     * @param actor
     * @param namespace
     */
    public TeaseScript(TeaseLib teaseLib, Actor actor, String namespace) {
        super(teaseLib, actor, namespace);
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
        MediaRenderer desktopItem = new RenderDesktopItem(path);
        addDeferred(desktopItem);
    }

    public void setSound(String path) {
        addDeferred(new RenderSound(path));
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
                path);
        addDeferred(renderBackgroundSound);
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
        addDeferred(new RenderDelay(seconds));
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
        renderMessage(message, teaseLib.speechSynthesizer);
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

    protected void renderMessage(Message message,
            TextToSpeechPlayer speechSynthesizer) {
        try {
            renderMessage(message, speechSynthesizer, displayImage, mood);
        } finally {
            displayImage = Message.DominantImage;
            mood = Mood.Neutral;
        }
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
        return reply(NoTimeout, choices);
    }

    /**
     * Display choices. Won't wait for mandatory parts to complete.
     * 
     * @param choices
     * @param timeout
     *            The timeout for the button set in seconds, or 0 if no timeout
     *            is desired
     * @return The button index, or TeaseLib.None if the buttons timed out
     */
    public String reply(final int timeout, final List<String> choices) {
        completeMandatory();
        Runnable delayFunction = timeout > NoTimeout ? new Runnable() {
            @Override
            public void run() {
                teaseLib.sleep(timeout, TimeUnit.SECONDS);
            }
        } : null;
        return showChoices(delayFunction, choices);
    }

    /**
     * @param choices
     * @param timeout
     *            The timeout for the button set in seconds, or
     *            TeaseScript.NoTimeout if no timeout is desired
     * @param scriptFunction
     * @return
     */
    public String reply(Runnable scriptFunction, final List<String> choices) {
        // To display buttons and to start scriptFunction at the same time,
        // completeAll() has to be called in order to finish all current
        // renderers
        completeAll();
        String chosen = showChoices(scriptFunction, choices);
        if (chosen == Timeout) {
            completeAll();
        } else {
            endAll();
        }
        return chosen;
    }

    public String reply(String choice, String... more) {
        return reply(NoTimeout, choice, more);
    }

    public String reply(final int timeout, String choice, String... more) {
        List<String> choices = buildChoicesFromArray(choice, more);
        return reply(timeout, choices);
    }

    public String reply(Runnable scriptFunction, String choice, String... more) {
        List<String> choices = buildChoicesFromArray(choice, more);
        return reply(scriptFunction, choices);
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

    public boolean showItems(String caption, List<Item> items,
            boolean allowCancel) {
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
