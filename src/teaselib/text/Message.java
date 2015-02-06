package teaselib.text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import teaselib.Actor;
import teaselib.Attitude;
import teaselib.TeaseScript;

public class Message {

    /**
     * Message types.
     */
    enum Type {
        Text, Image, Sound, DesktopItem, Mood, Keyword, Delay
    }

    /**
     * Usage: Delay n Insert a pause, n seconds long
     */
    public final static String Delay = "delay";

    /**
     * Declare he mandatory part of the message to be completed. If the message
     * is followed by a choice, the choice buttons are displayed, but the
     * message will continue to render its optional part. Nice to comment
     * actions when the slave is busy for a longer duration.
     */
    public final static String ShowChoices = "ShowChoices";

    public final static String[] Keywords = { Delay, ShowChoices,
            TeaseScript.DominantImage, TeaseScript.NoImage };

    public final Actor actor;

    private final Parts parts;

    public Message(Actor actor) {
        this.parts = new Parts();
        this.actor = actor;
    }

    // TODO character type cannot be string

    /**
     * @param message
     *            The message to render, or null to display no message
     */
    public Message(Actor actor, String message) {
        parts = new Parts();
        if (message != null) {
            parts.add(message);
        }
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
     * paragraph until a sencente is completed. To add multiple sentences to a
     * single paragraph, add text containing the two sentences.
     * 
     * File names and keywords are added as speparate paragraphs.
     * 
     * @param text
     */
    public void add(String text) {
        if (text == null)
            throw new IllegalArgumentException();
        parts.add(text);
    }

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
            Iterator<Part> i = parts.iterator();
            StringBuilder format = new StringBuilder();
            for (; i.hasNext();) {
                Part part = i.next();
                boolean append = all || part.type == Type.Text;
                if (append) {
                    format.append(part.value);
                }
                if (i.hasNext()) {
                    format.append(newLine);
                }
            }
            return format.toString();
        }
    }

    public List<Part> getParagraphs() {
        List<Part> p = new ArrayList<Part>();
        for (Part s : parts) {
            p.add(s);
        }
        return p;
    }

    public static boolean isFile(String m) {
        int s = m.length();
        int i = m.lastIndexOf(".");
        if (i > 0 && i != s - 1) {
            // This could certainly be done better than just assuming
            // file extensions are non-space letters after a dot
            return m.substring(i, s - 1).indexOf(" ") < 0;
        } else {
            return false;
        }
    }

    public static boolean isFile(Type t) {
        return t == Type.Image || t == Type.Sound || t == Type.DesktopItem;
    }

    public static boolean isImage(String m) {
        return m.endsWith(".png") || m.endsWith(".jpg");
    }

    public static boolean isSound(String m) {
        return m.endsWith(".wav") || m.endsWith(".ogg") || m.endsWith(".mp3");
    }

    public static boolean isMood(String m) {
        return Attitude.matches(m);
    }

    public static boolean isKeyword(String m) {
        return keywordFrom(m) != null;
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

    public static boolean endOfSentence(String line) {
        boolean ending = line.endsWith(":") || line.endsWith(".")
                || line.endsWith("!") || line.endsWith("?")
                || line.endsWith(">");
        return ending;
    }

    public static Type determineType(String m) {
        if (isMood(m)) {
            return Type.Mood;
        } else if (isFile(m)) {
            if (isImage(m)) {
                return Type.Image;
            } else if (isSound(m)) {
                return Type.Sound;
            } else {
                return Type.DesktopItem;
            }
        } else if (isKeyword(m)) {
            if (m.toLowerCase().startsWith(Delay)) {
                return Type.Delay;
            } else {
                return Type.Keyword;
            }
        } else {
            // TODO Keyword check
            return Type.Text;
        }
    }

    public class Part {
        public final String value;
        public final Type type;

        public Part(String value, Type type) {
            this.value = value;
            this.type = type;
        }

        public boolean isFile() {
            return Message.isFile(type);
        }
    }

    class Parts implements Iterable<Part> {

        private final List<Part> p = new Vector<Part>();

        private boolean newParagraph = false;

        public boolean isEmpty() {
            return p.isEmpty();
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

        @Override
        public Iterator<Part> iterator() {
            return p.iterator();
        }

        public void add(String text) {
            final Type type = determineType(text);
            if (type == Type.Keyword) {
                text = keywordFrom(text);
            }
            boolean requiresSeparateLine = type != Type.Text;
            boolean requiresNewParagraphBefore = requiresSeparateLine;
            boolean requiresNewParagraphAfter = requiresSeparateLine
                    || endOfSentence(text);
            if (p.isEmpty()) {
                // First
                p.add(new Part(text, type));
                newParagraph = requiresNewParagraphAfter;
            } else if (requiresNewParagraphBefore) {
                // File as new paragraph
                p.add(new Part(text, type));
                newParagraph = true;
            } else if (newParagraph) {
                p.add(new Part(text, type));
                newParagraph = requiresNewParagraphAfter;
            } else {
                // Append
                int i = p.size() - 1;
                p.set(i, new Part(p.get(i).value + " " + text, type));
                newParagraph = requiresNewParagraphAfter;
            }
        }
    }
}
