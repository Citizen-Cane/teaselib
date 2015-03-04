package teaselib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import teaselib.audio.RenderSound;
import teaselib.image.ImageIterator;
import teaselib.persistence.Clothing;
import teaselib.persistence.Item;
import teaselib.persistence.Toys;
import teaselib.text.Message;
import teaselib.text.RenderDelay;
import teaselib.texttospeech.TextToSpeechPlayer;
import teaselib.userinterface.MediaRenderer;
import teaselib.util.RenderDesktopItem;

public abstract class TeaseScript extends TeaseScriptBase implements Runnable {

    public static final int NoTimeout = 0;

    public ImageIterator dominantImages = null;
    private String displayImage = DominantImage;
    public final static String NoImage = "NoImage";
    public final static String DominantImage = "DominantImage";

    private String attitude = Attitude.Neutral;

    public final Actor actor;

    public TeaseScript(TeaseLib teaseLib, String locale) {
        this(teaseLib, new Actor("Dominant", locale));
    }

    public TeaseScript(TeaseLib teaseLib, Actor actor) {
        super(teaseLib, actor.locale);
        this.actor = actor;
    }

    /**
     * Return a random number
     * 
     * @param min
     * @param max
     * @return A value in the interval [min, max]
     */
    public int getRandom(int min, int max) {
        return teaseLib.host.getRandom(min, max);
    }

    /**
     * Renders the image denoted by the path. The image will not be displayed
     * immediately but during the next message rendering. This is because if no
     * image is specified, an image of the dominant character will be used.
     * 
     * @param path
     *            The path to the image
     */
    public void showImage(String path) {
        if (path.equalsIgnoreCase(DominantImage)) {
            displayImage = DominantImage;
        } else if (path.equalsIgnoreCase(NoImage)) {
            displayImage = NoImage;
        } else {
            displayImage = path;
        }
    }

    public void showDesktopItem(String path) {
        MediaRenderer desktopItem = new RenderDesktopItem(path);
        deferredRenderers.add(desktopItem);
    }

    public void playSound(String path) {
        deferredRenderers.add(new RenderSound(path));
    }

    void setAttitude(String attitude) {
        this.attitude = attitude;
    }

    /**
     * Wait the requested numbers of seconds after displaying a message.
     * 
     * @param seconds
     *            How long to wait.
     */
    public void delay(int seconds) {
        deferredRenderers.add(new RenderDelay(seconds));
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
        renderMessage(message, speechSynthesizer);
    }

    /**
     * Show instructional text, this is not spoken, just displayed
     * 
     * @param message
     *            The text top be displayed
     */
    public void show(String message) {
        renderMessage(new Message(actor, message), null);
    }

    protected void renderMessage(Message message,
            TextToSpeechPlayer speechSynthesizer) {
        try {
            renderMessage(message, speechSynthesizer, dominantImages,
                    displayImage, attitude);
        } finally {
            displayImage = DominantImage;
            attitude = Attitude.Neutral;
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
    public String choose(List<String> choices) {
        return choose(NoTimeout, choices);
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
    public String choose(final int timeout, final List<String> choices) {
        completeMandatory();
        Runnable delayFunction = timeout > NoTimeout ? new Runnable() {
            @Override
            public void run() {
                teaseLib.host.sleep(timeout * 1000);
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
    public String choose(Runnable scriptFunction, List<String> choices) {
        // To display buttons and to start scriptFunction at the same time,
        // completeAll() has to be called
        // in advance in order to finish all previous render commands,
        completeAll();
        String choice = showChoices(scriptFunction, choices);
        if (choice == Timeout) {
            renderQueue.completeAll();
        } else {
            renderQueue.endAll();
        }
        return choice;
    }

    public String choose(String... choices) {
        return choose(NoTimeout, new ArrayList<String>(Arrays.asList(choices)));
    }

    public String choose(Runnable scriptFunction, String... choices) {
        return choose(scriptFunction,
                new ArrayList<String>(Arrays.asList(choices)));
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
    public List<Boolean> showCheckboxes(String caption, List<String> choices,
            List<Boolean> values, boolean allowCancel) {
        List<Boolean> results = teaseLib.host.showCheckboxes(caption, choices,
                values, false);
        renderQueue.endAll();
        return results;
    }

    public Item get(Toys item) {
        return teaseLib.persistence.get(item);
    }

    public Item get(Clothing item) {
        return teaseLib.persistence.get(item);
    }

    public boolean isAvailable(Toys... toys) {
        for (Toys toy : toys) {
            Item item = teaseLib.persistence.get(toy);
            if (item.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    public List<Item> get(Toys... toys) {
        List<Item> items = new ArrayList<Item>();
        for (Toys toy : toys) {
            Item item = teaseLib.persistence.get(toy);
            items.add(item);
        }
        return items;
    }

    public boolean isAvailable(Clothing... clothes) {
        for (Clothing clothing : clothes) {
            Item item = teaseLib.persistence.get(clothing);
            if (item.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    public List<Item> get(Clothing... clothes) {
        List<Item> items = new ArrayList<Item>();
        for (Clothing clothing : clothes) {
            Item item = teaseLib.persistence.get(clothing);
            items.add(item);
        }
        return items;
    }

    public boolean isAvailable(List<Item> items) {
        for (Item item : items) {
            if (item.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    public boolean isAvailable(Item... items) {
        for (Item item : items) {
            if (item.isAvailable()) {
                return true;
            }
        }
        return false;
    }
}
