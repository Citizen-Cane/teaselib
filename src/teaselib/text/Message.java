package teaselib.text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import teaselib.Actor;
import teaselib.Attitude;
import teaselib.TeaseScript;

public class Message {

    public final static String Delay = "delay";

    public final Actor actor;

    private final XParagraphs paragraphs;

    public Message(Actor actor) {
        this.paragraphs = new XParagraphs();
        this.actor = actor;
    }

    // TODO character type cannot be string

    /**
     * @param message
     *            The message to render, or null to display no message
     */
    public Message(Actor actor, String message) {
        paragraphs = new XParagraphs();
        if (message != null) {
            paragraphs.add(message);
        }
        this.actor = actor;
    }

    /**
     * @param message
     *            The message to render, or null or an empty vector to display
     *            no message
     */
    public Message(Actor actor, String... message) {
        paragraphs = new XParagraphs();
        if (message != null) {
            paragraphs.addAll(message);
        }
        this.actor = actor;
    }

    /**
     * @param message
     *            The message to render, or null or an empty vector to display
     *            no message
     */
    public Message(Actor actor, List<String> message) {
        this.paragraphs = new XParagraphs();
        if (message != null) {
            paragraphs.addAll(message);
        }
        this.actor = actor;
    }

    public boolean isEmpty() {
        return paragraphs.isEmpty();
    }

    public Iterator<String> iterator() {
        return paragraphs.iterator();
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
        paragraphs.add(text);
    }

    public String toString() {
        return buildString("\n\n", true);
    }

    public String toHashString() {
        return buildString("\n", false);
    }

    private String buildString(String newLine, boolean all) {
        if (paragraphs == null) {
            return "";
        } else if (paragraphs.isEmpty()) {
            return "";
        } else {
            Iterator<String> i = paragraphs.iterator();
            StringBuilder format = new StringBuilder(i.next());
            for (; i.hasNext();) {
                String line = i.next();
                boolean append = all
                        || (!isFile(line) && !isKeyword(line) && !isMood(line));
                if (append) {
                    format.append(line);
                }
                if (i.hasNext()) {
                    format.append(newLine);
                }
            }
            return format.toString();
        }
    }

    public List<String> getParagraphs() {
        List<String> p = new ArrayList<>();
        for (String s : paragraphs) {
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
        return m.toLowerCase().startsWith(Delay)
                || m.equalsIgnoreCase(TeaseScript.NoImage)
                || m.equalsIgnoreCase(TeaseScript.DominantImage);
    }

    public static boolean endOfSentence(String line) {
        boolean ending = line.endsWith(":") || line.endsWith(".")
                || line.endsWith("!") || line.endsWith("?")
                || line.endsWith(">");
        return ending;
    }

    class XParagraphs implements Iterable<String> {
        private final List<String> p = new Vector<>();

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
        public Iterator<String> iterator() {
            return p.iterator();
        }

        public void add(String m) {
            boolean requiresSeparateLine = isFile(m) || isMood(m)
                    || isKeyword(m);
            boolean requiresNewParagraphBefore = requiresSeparateLine;
            boolean requiresNewParagraphAfter = requiresSeparateLine
                    || endOfSentence(m);
            if (p.isEmpty()) {
                // First
                p.add(m);
                newParagraph = requiresNewParagraphAfter;
            } else if (requiresNewParagraphBefore) {
                // File as new paragraph
                p.add(m);
                newParagraph = true;
            } else if (newParagraph) {
                p.add(m);
                newParagraph = requiresNewParagraphAfter;
                ;
            } else {
                // Append
                int i = p.size() - 1;
                p.set(i, p.get(i) + " " + m);
                newParagraph = requiresNewParagraphAfter;
            }
        }
    }
}
