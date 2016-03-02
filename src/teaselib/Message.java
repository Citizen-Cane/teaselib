package teaselib;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class Message {

    /**
     * Message types.
     */
    @SuppressWarnings("hiding")
    public enum Type {
        /**
         * Text
         */
        Text,
        /**
         * An image
         */
        Image,
        /**
         * Background audio, the message continues to render.
         */
        BackgroundSound,
        /**
         * Foreground audio, message rendering is paused until the sound
         * completes.
         */
        Sound,
        /**
         * Similar to {@link Type#BackgroundSoundSound} but disables speech
         * recognition to avoid wrong recognitions.
         */
        Speech,
        /**
         * Show item on the desktop.
         */
        DesktopItem,
        /**
         * Sets the mood for the following text & image
         */
        Mood,
        /**
         * A special keyword as listed in {@link Message#Keywords}.
         */
        Keyword,
        /**
         * Waits the specified duration before resuming message rendering.
         */
        Delay,
        /**
         * Renders text with a leading bullet, useful for enumerations
         */
        Item
    }

    /**
     * Usage: Delay n Insert a pause, n seconds long
     */
    public final static String Delay = "delay";

    public final static String Delay2s = "delay 2";
    public final static String Delay2to5s = "delay 2 5";
    public final static String Delay3s = "delay 3";
    public final static String Delay5s = "delay 5";
    public final static String Delay5to10s = "delay 5 10";
    public final static String Delay7s = "delay 7";
    public final static String Delay10s = "delay 10";
    public final static String Delay10to15s = "delay 10 15";
    public final static String Delay10to20s = "delay 10 20";
    public final static String Delay15s = "delay 15";
    public final static String Delay15to30s = "delay 15 30";
    public final static String Delay20s = "delay 20";
    public final static String Delay25s = "delay 25";
    public final static String Delay30s = "delay 30";
    public final static String Delay30to45s = "delay 30 45";
    public final static String Delay30to50s = "delay 30 50";
    public final static String Delay30to60s = "delay 30 60";
    public final static String Delay35s = "delay 35";
    public final static String Delay40s = "delay 40";
    public final static String Delay45s = "delay 45";
    public final static String Delay45to60s = "delay 45 60";
    public final static String Delay50s = "delay 50";
    public final static String Delay55s = "delay 55";
    public final static String Delay60s = "delay 60";
    public final static String Delay60to120s = "delay 60 120";

    /**
     * Don't display an image
     */
    public final static String NoImage = "NoImage";

    /**
     * Display an image of the dominant, the actor that speaks the message
     */
    public final static String DominantImage = "DominantImage";

    /**
     * Execute the desktop action
     */
    public final static String ShowOnDesktop = "showondesktop";

    /**
     * Declare he mandatory part of the message to be completed. If the message
     * is followed by a choice, the choice buttons are displayed, but the
     * message will continue to render its optional part. Nice to comment
     * actions when the slave is busy for a longer duration.
     */
    public final static String ShowChoices = "showChoices";

    public final static String AwaitSoundCompletion = "awaitSoundCompletion";

    public final static String Bullet = "°";

    public final static String[] Keywords = { Delay, ShowOnDesktop, ShowChoices,
            AwaitSoundCompletion, DominantImage, NoImage, Bullet };

    public final Actor actor;

    public static Set<String> EndOfSentenceCharacters = new HashSet<String>(
            Arrays.asList(":", ".", "!", "?"));
    public static Set<String> MainClauseAppendableCharacters = new HashSet<String>(
            Arrays.asList("\"", ">", ",", ";", "-"));

    private final Parts parts;

    public Message(Actor actor) {
        this.parts = new Parts();
        this.actor = actor;
    }

    /**
     * @param message
     *            The message to render, or null or an empty vector to display
     *            no message
     */
    public Message(Actor actor, String... message) {
        parts = new Parts();
        if (message != null) {
            parts.addAll(message);
        }
        this.actor = actor;
    }

    /**
     * @param message
     *            The message to render, or null or an empty vector to display
     *            no message
     */
    public Message(Actor actor, List<String> message) {
        this.parts = new Parts();
        if (message != null) {
            parts.addAll(message);
        }
        this.actor = actor;
    }

    public boolean isEmpty() {
        return parts.isEmpty();
    }

    public Iterator<Part> iterator() {
        return parts.iterator();
    }

    /**
     * Add text to the message. Text is automatically appended to the last
     * paragraph until a sentence is completed. To add multiple sentences to a
     * single paragraph, add text containing the two sentences.
     * 
     * File names and keywords are added as separate paragraphs.
     * 
     * @param text
     */
    public void add(String... text) {
        if (text == null)
            throw new IllegalArgumentException();
        for (String t : text) {
            if (t == null)
                throw new IllegalArgumentException();
            parts.add(t);
        }
    }

    public void add(Message message) {
        if (message == null)
            throw new IllegalArgumentException();
        for (Part part : message.getParts()) {
            parts.add(part);
        }
    }

    /**
     * Use with caution, since parts are not concatenated to sentences, which
     * may result in collisions when prerecording speech
     * 
     * @param part
     */
    public void add(Part part) {
        parts.add(part);
    }

    public void add(Type type, String value) {
        parts.add(type, value);
    }

    @Override
    public String toString() {
        return buildString("\n\n", true);
    }

    public String toHashString() {
        return buildString("\n", false);
    }

    private String buildString(String newLine, boolean all) {
        if (parts == null) {
            return "";
        } else if (parts.isEmpty()) {
            return "";
        } else {
            StringBuilder messageString = new StringBuilder();
            for (Iterator<Part> i = parts.iterator(); i.hasNext();) {
                Part part = i.next();
                boolean appendPart = all || part.type == Type.Text
                        || part.type == Type.Mood;
                if (appendPart) {
                    if (messageString.length() > 0) {
                        messageString.append(newLine);
                    }
                    messageString.append(part.value);
                }
            }
            return messageString.toString();
        }
    }

    public Parts getParts() {
        return parts;
    }

    public static boolean isFile(String m) {
        int s = m.length();
        int i = m.lastIndexOf(".");
        if (i > 0 && i != s - 1) {
            // Inline-files must have extensions with at least one alphanumeric
            // letter after the dot
            String extension = m.substring(i + 1, m.length());
            if (extension.matches("[A-Za-z0-9]+")) {
                // This could certainly be done better than just assuming
                // file extensions are non-space letters after a dot
                return m.substring(i, s - 1).indexOf(" ") < 0;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean isFile(Type t) {
        return t == Type.Image || t == Type.BackgroundSound || t == Type.Sound
                || t == Type.Speech || t == Type.DesktopItem;
    }

    public static boolean isImage(String m) {
        return m.endsWith(".png") || m.endsWith(".jpg");
    }

    public static boolean isSound(String m) {
        return m.endsWith(".wav") || m.endsWith(".ogg") || m.endsWith(".mp3");
    }

    public static boolean isMood(String m) {
        return Mood.isMood(m);
    }

    public static boolean isKeyword(String m) {
        return !endOf(m, EndOfSentenceCharacters) && keywordFrom(m) != null;
    }

    public static String keywordFrom(String m) {
        final String candidate;
        int i = m.indexOf(' ');
        if (i < 1) {
            candidate = m.toLowerCase();
        } else {
            candidate = m.substring(0, i).toLowerCase();
        }
        for (String keyword : Keywords) {
            if (candidate.equalsIgnoreCase(keyword)) {
                return keyword;
            }
        }
        return null;
    }

    public static boolean endOf(String line, Collection<String> endOf) {
        for (String s : endOf) {
            if (line.endsWith(s))
                return true;
        }
        // Check sentences with ending quote
        if (line.endsWith("\"")) {
            for (String s : endOf) {
                if (line.endsWith(s + "\""))
                    return true;
            }
        }
        return false;
    }

    public static Type determineType(String m) {
        if (m.isEmpty()) {
            return Type.Text;
        }
        String mToLower = m.toLowerCase();
        if (isMood(mToLower)) {
            return Type.Mood;
        } else if (isKeyword(mToLower)) {
            // For commands with parameters, the command is stored in the type,
            // and the parameters as the text
            if (mToLower.startsWith(Delay)) {
                return Type.Delay;
            } else if (mToLower.startsWith(ShowOnDesktop)) {
                return Type.DesktopItem;
            } else if (mToLower.equals(Message.Bullet)) {
                return Type.Item;
            } else {
                return Type.Keyword;
            }
            // keywords and commands must be processed before files,
            // since commands may contain file arguments
        } else if (isFile(mToLower)) {
            if (isImage(mToLower)) {
                return Type.Image;
            } else if (isSound(mToLower)) {
                return Type.BackgroundSound;
            } else {
                return Type.DesktopItem;
            }
        } else {
            // Catch misspelled keywords - not foolproof since we decided to
            // implement keywords as natural words without using special
            // characters
            int s = words(m).length;
            // s == 2 also catches misspelled delay commands
            if (s > 2 || endOf(m, EndOfSentenceCharacters)
                    || endOf(m, MainClauseAppendableCharacters)) {
                return Type.Text;
            } else {
                // Throwing an exception here to avoid speaking keywords in
                // PCMPlayer is a bit harsh, because it's a script issue.
                // However in the future we might need to make this configurable
                // if a script language needs this feature.
                // throw new IllegalArgumentException(
                // "I Can't believe this is supposed to be a text message: "
                // + m);
                // However when writing scripts in java text is evaluated only
                // when say() or show() is called(...),
                // so we can reference keywords via static string constants,
                // instead of using plain text, and therefore we're safe to
                // assume that if it's not a keyword, then it's text
                return Type.Text;
            }
        }
    }

    private static String[] words(String m) {
        return m.split(" |\t");
    }

    public class Part {
        public final Type type;
        public final String value;

        public Part(Type type, String value) {
            this.value = value;
            this.type = type;
        }

        public boolean isFile() {
            return Message.isFile(type);
        }

        @Override
        public String toString() {
            return type.name() + "=" + value;
        }
    }

    public class Parts implements Iterable<Part> {

        private final List<Part> p = new Vector<Part>();

        public boolean isEmpty() {
            return p.isEmpty();
        }

        public void clear() {
            p.clear();
        }

        public void addAll(List<String> paragraphs) {
            for (String s : paragraphs) {
                add(s);
            }
        }

        public void addAll(String[] paragraphs) {
            for (String s : paragraphs) {
                add(s);
            }
        }

        public void addAll(Parts parts) {
            for (Part part : parts) {
                add(part);
            }
        }

        @Override
        public Iterator<Part> iterator() {
            return p.iterator();
        }

        private void add(Part part) {
            if (part.type == Type.Text) {
                p.add(part);
            } else if (part.type == Type.Mood) {
                if (!p.isEmpty()) {
                    int i = p.size() - 1;
                    Part previous = p.get(i);
                    if (previous.type == Type.Mood) {
                        p.set(i, part);
                    } else {
                        p.add(part);
                    }
                } else {
                    p.add(part);
                }
            } else {
                p.add(part);
            }
        }

        public void add(String text) {
            if (text == null)
                throw new IllegalArgumentException(text);
            Type type = determineType(text);
            add(type, text);
        }

        public void add(Type type, String value) {
            value = value.trim();
            if (type == Type.Keyword) {
                value = keywordFrom(value);
            }
            add(new Part(type, value));
        }

        public int size() {
            return p.size();
        }

        public Part get(int index) {
            return p.get(index);
        }

        @Override
        public String toString() {
            return "size=" + size() + ", " + p.toString();
        }
    }

    public Message joinSentences() {
        Parts newParts = new Parts();
        Iterator<Part> parts = this.parts.iterator();
        Part sentence = null;
        while (parts.hasNext()) {
            Part part = parts.next();
            if (part.type == Type.Text) {
                if (!endOf(part.value, EndOfSentenceCharacters)
                        && !endOf(part.value, MainClauseAppendableCharacters)) {
                    if (sentence == null) {
                        sentence = part;
                    } else {
                        sentence = new Part(Type.Text,
                                sentence.value + " " + part.value);
                    }
                } else {
                    if (sentence != null) {
                        newParts.add(Type.Text,
                                sentence.value + " " + part.value);
                        sentence = null;
                    } else {
                        newParts.add(part);
                    }
                }
            } else {
                newParts.add(part);
            }
        }
        this.parts.clear();
        this.parts.addAll(newParts);
        return this;
    }

    public Message readAloud() {
        Parts newParts = new Parts();
        Iterator<Part> parts = this.parts.iterator();
        boolean readAloud = false;
        while (parts.hasNext()) {
            Part part = parts.next();
            if (part.type == Type.Text) {
                String text = part.value;
                boolean readAloudStart = text.startsWith("\"");
                boolean readAloudEnd = text.endsWith("\"")
                        || text.endsWith("\"."); // todo
                                                 // generalize
                if (readAloudStart && !readAloud) {
                    newParts.add(new Part(Type.Mood, Mood.Reading));
                    readAloud = true;
                } else if (readAloud) {
                    // Repeat for each text part
                    newParts.add(new Part(Type.Mood, Mood.Reading));
                }
                newParts.add(part);
                if (readAloudEnd && readAloud) {
                    newParts.add(new Part(Type.Mood, Mood.Neutral));
                    readAloud = false;
                }
            } else {
                newParts.add(part);
            }
        }
        this.parts.clear();
        this.parts.addAll(newParts);
        return this;
    }
}
