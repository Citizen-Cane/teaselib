package teaselib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import teaselib.audio.RenderSound;
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

    // todo move images to actor, remove images from renderMessage constructor

    public final static String NoImage = "NoImage";
    public final static String DominantImage = "DominantImage";

    public final Actor actor;
    public final String namespace;

    private String mood = Mood.Neutral;
    private String displayImage = DominantImage;

    public TeaseScript(TeaseScript teaseScript, Actor actor) {
        super(teaseScript.teaseLib, actor.locale);
        this.actor = actor;
        this.namespace = teaseScript.namespace;
    }

    public TeaseScript(TeaseLib teaseLib, String locale, String namespace) {
        this(teaseLib, new Actor("Dominant", locale), namespace);
    }

    public TeaseScript(TeaseLib teaseLib, Actor actor, String namespace) {
        super(teaseLib, actor.locale);
        this.actor = actor;
        this.namespace = namespace;
    }

    /**
     * Return a random number
     * 
     * @param min
     * @param max
     * @return A value in the interval [min, max]
     */
    public int random(int min, int max) {
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
        if (path == null) {
            displayImage = NoImage;
        } else if (path.equalsIgnoreCase(DominantImage)) {
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

    void setMood(String mood) {
        this.mood = mood;
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
            renderMessage(message, speechSynthesizer, displayImage, mood);
        } finally {
            displayImage = DominantImage;
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
    public String reply(List<String> choices) {
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
    public String reply(Runnable scriptFunction, List<String> choices) {
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

    public String reply(String... choices) {
        return reply(NoTimeout, choices);
    }

    public String reply(final int timeout, String... choices) {
        return reply(timeout, new ArrayList<String>(Arrays.asList(choices)));
    }

    public String reply(Runnable scriptFunction, String... choices) {
        return reply(scriptFunction,
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
    public List<Boolean> showItems(String caption, List<String> choices,
            List<Boolean> values, boolean allowCancel) {
        List<Boolean> results = teaseLib.host.showCheckboxes(caption, choices,
                values, allowCancel);
        renderQueue.endAll();
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

    public Item get(Toys item) {
        return teaseLib.persistence.get(item);
    }

    public Item get(Clothing item) {
        return teaseLib.persistence.get(item);
    }

    public boolean isAnyAvailable(Toys... toys) {
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

    public boolean isAnyAvailable(Clothing... clothes) {
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

    public boolean isAnyAvailable(List<Item> items) {
        for (Item item : items) {
            if (item.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    public boolean isAnyAvailable(Item... items) {
        for (Item item : items) {
            if (item.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    protected class PersistentValue {
        protected final String Name;
        protected final String property;

        public PersistentValue(String name) {
            this.Name = name;
            this.property = makeProperty(name);
        }
    }

    /**
     * @author someone
     * 
     *         A persistent boolean value, start value is false
     */
    public class PersistentFlag extends PersistentValue {
        public PersistentFlag(String name) {
            super(name);
        }

        public boolean get() {
            return teaseLib.persistence.get(property) == "1";
        }

        public void set(boolean value) {
            teaseLib.persistence.set(property, value);
        }
    }

    /**
     * @author someone
     * 
     *         A persistent integer value, start value is 0
     */
    public class PersistentNumber extends PersistentValue {
        public PersistentNumber(String name) {
            super(name);
        }

        public int get() {
            try {
                return Integer.parseInt(teaseLib.persistence.get(property));
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        public void set(int value) {
            teaseLib.persistence.set(property, Integer.toString(value));
        }
    }

    /**
     * @author someone
     * 
     *         A persistent String value, start value is the empty string
     */
    public class PersistentString extends PersistentValue {
        public PersistentString(String name) {
            super(name);
        }

        public String get() {
            return teaseLib.persistence.get(property);
        }

        public void set(String value) {
            teaseLib.persistence.set(property, value);
        }
    }

    private String makeProperty(String name) {
        return namespace + "." + name;
    }

    public boolean flag(String name) {
        return new PersistentFlag(name).get();
    }

    public void set(String name, boolean value) {
        new PersistentFlag(name).set(value);
    }

    public void set(String name, int value) {
        new PersistentNumber(name).set(value);
    }

    public void set(String name, String value) {
        new PersistentString(name).set(value);
    }
}
