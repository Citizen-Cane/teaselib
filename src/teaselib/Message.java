package teaselib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import teaselib.core.AbstractMessage;

public class Message extends AbstractMessage {
    /**
     * Message types.
     */
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
         * Foreground audio, message rendering is paused until the sound completes.
         */
        Sound,
        /**
         * Similar to {@link Type#BackgroundSoundSound} but disables speech recognition to avoid wrong recognitions.
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

        ;

        public static final Set<Message.Type> TextTypes = new HashSet<>(
                Arrays.asList(Message.Type.Text, Message.Type.Item));

        public static final Set<Message.Type> DisplayTypes = new HashSet<>(
                Arrays.asList(Message.Type.Text, Message.Type.Item, Message.Type.Image));

        public static final Set<Message.Type> AudioTypes = new HashSet<>(
                Arrays.asList(Message.Type.Sound, Message.Type.BackgroundSound, Message.Type.Speech));

        public static final Set<Type> FileTypes = new HashSet<>(
                Arrays.asList(Type.BackgroundSound, Type.Sound, Type.Speech, Type.Image, Type.DesktopItem));

        public static boolean isFile(Type t) {
            return (Type.FileTypes.contains(t));
        }

        public static boolean isImage(String m) {
            return m.endsWith(".png") || m.endsWith(".jpg");
        }

        public static boolean isSound(String m) {
            return m.endsWith(".wav") || m.endsWith(".ogg") || m.endsWith(".mp3");
        }

        public static boolean isMood(String m) {
            return teaselib.Mood.isMood(m);
        }

        public static boolean isKeyword(String m) {
            return !endOf(m, EndOfSentenceCharacters) && keywordFrom(m) != null;
        }
    }

    /**
     * Usage: Delay n Insert a pause, n seconds long
     */
    public static final String Delay = "delay";

    public static String delay(int n) {
        return Delay + " " + n;
    }

    public static String delay(int n, int m) {
        return Delay + " " + n + " " + m;
    }

    public static final String Delay2s = "delay 2";
    public static final String Delay2to5s = "delay 2 5";
    public static final String Delay3s = "delay 3";
    public static final String Delay5s = "delay 5";
    public static final String Delay5to10s = "delay 5 10";
    public static final String Delay7s = "delay 7";
    public static final String Delay10s = "delay 10";
    public static final String Delay10to15s = "delay 10 15";
    public static final String Delay10to20s = "delay 10 20";
    public static final String Delay15s = "delay 15";
    public static final String Delay15to30s = "delay 15 30";
    public static final String Delay15to45s = "delay 15 45";
    public static final String Delay20s = "delay 20";
    public static final String Delay25s = "delay 25";
    public static final String Delay30s = "delay 30";
    public static final String Delay30to45s = "delay 30 45";
    public static final String Delay30to50s = "delay 30 50";
    public static final String Delay30to60s = "delay 30 60";
    public static final String Delay35s = "delay 35";
    public static final String Delay40s = "delay 40";
    public static final String Delay45s = "delay 45";
    public static final String Delay45to60s = "delay 45 60";
    public static final String Delay50s = "delay 50";
    public static final String Delay55s = "delay 55";
    public static final String Delay60s = "delay 60";
    public static final String Delay90s = "delay 90";
    public static final String Delay60to120s = "delay 60 120";
    public static final String Delay90to150s = "delay 90 120";
    public static final String Delay90to180s = "delay 90 120";
    public static final String Delay120s = "delay 120";

    /**
     * Don't display an image
     */
    public static final String NoImage = "NoImage";

    /**
     * Display an image of the the actor that speaks the message
     */
    public static final String ActorImage = "ActorImage";

    /**
     * Execute the desktop action
     */
    public static final String ShowOnDesktop = "showondesktop";

    /**
     * Declare he mandatory part of the message to be completed. If the message is followed by a prompt, the prompt's
     * choices are realized, but the message will continue to render. Nice to comment actions while the player follows
     * orders.
     */
    public static final String ShowChoices = "showChoices";

    public static final String AwaitSoundCompletion = "awaitSoundCompletion";

    public static final String Bullet = "°";

    public static final String BackgroundSound = "BackgroundSound";

    static final String[] Keywords = { Delay, ShowOnDesktop, ShowChoices, AwaitSoundCompletion, Bullet,
            BackgroundSound };

    static final Set<String> EndOfSentenceCharacters = new HashSet<>(Arrays.asList(":", ".", "!", "?"));
    public static final Set<String> MainClauseAppendableCharacters = new HashSet<>(
            Arrays.asList("\"", ">", ",", ";", "-"));

    private static final Set<Type> ParagraphStart = new HashSet<>(
            Arrays.asList(Type.Image, Type.Mood, Type.Text, Type.Speech));

    private static final Set<String> ImageKeywords = new HashSet<>(
            Arrays.asList(ActorImage.toLowerCase(), NoImage.toLowerCase()));

    public final Actor actor;

    public Message(Actor actor) {
        this.actor = actor;
    }

    /**
     * @param message
     *            The message to render, or null or an empty list to display no message
     */
    public Message(Actor actor, String... message) {
        this.actor = actor;
        addAll(message);
    }

    /**
     * @param paragraphs
     *            The message to render, or null or an empty list to display no message
     */
    public Message(Actor actor, List<String> paragraphs) {
        this.actor = actor;
        addAll(paragraphs);
    }

    public Message(Actor actor, AbstractMessage message) {
        this.actor = actor;
        addAll(message);
    }

    /**
     * Add text to the message. Text is automatically appended to the last paragraph until a sentence is completed. To
     * add multiple sentences to a single paragraph, add text containing the two sentences.
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
            add(t);
        }
    }

    public static boolean isFile(String m) {
        int s = m.length();
        int i = m.lastIndexOf('.');

        // Don't interpret scripting error messages and the like as resources
        int j = m.indexOf(".");
        if (i != j && !m.startsWith("./")) {
            return false;
        }

        if (i > 0 && i != s - 1) {
            // Inline-files must have extensions with at least one alphanumeric
            // letter after the dot
            String extension = m.substring(i + 1, m.length());
            if (extension.matches("[A-Za-z0-9]+")) {
                // This could certainly be done better than just assuming
                // file extensions are non-space letters after a dot
                return m.substring(i, s - 1).indexOf(' ') < 0;
            } else {
                return false;
            }
        } else {
            return false;
        }
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
        if (ImageKeywords.contains(mToLower)) {
            return Type.Image;
        } else if (Type.isMood(mToLower)) {
            return Type.Mood;
        } else if (Type.isKeyword(mToLower)) {
            // For commands with parameters, the command is stored in the type,
            // and the parameters as the text
            String keyword = keywordFrom(m);
            if (keyword == Delay) {
                return Type.Delay;
            } else if (keyword == ShowOnDesktop) {
                return Type.DesktopItem;
            } else if (keyword == BackgroundSound) {
                return Type.BackgroundSound;
            } else if (keyword == Message.Bullet) {
                return Type.Item;
            } else {
                return Type.Keyword;
            }
            // keywords and commands must be processed before files,
            // since commands may contain file arguments
        } else if (isFile(mToLower)) {
            if (Type.isImage(mToLower)) {
                return Type.Image;
            } else if (Type.isSound(mToLower)) {
                return Type.Sound;
            } else {
                return Type.DesktopItem;
            }
        } else {
            // Catch misspelled keywords - not foolproof since we decided to
            // implement keywords as natural words without using special
            // characters
            int s = words(m).length;
            // s == 2 also catches misspelled delay commands
            if (s > 2 || endOf(m, EndOfSentenceCharacters) || endOf(m, MainClauseAppendableCharacters)) {
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

    public Message joinSentences() {
        AbstractMessage newParts = new AbstractMessage();
        Iterator<MessagePart> parts = iterator();
        MessagePart sentence = null;
        while (parts.hasNext()) {
            MessagePart part = parts.next();
            if (part.type == Type.Text) {
                if (!endOf(part.value, EndOfSentenceCharacters) && !endOf(part.value, MainClauseAppendableCharacters)) {
                    if (sentence == null) {
                        sentence = part;
                    } else {
                        sentence = new MessagePart(Type.Text, sentence.value + " " + part.value);
                    }
                } else {
                    if (sentence != null) {
                        newParts.add(Type.Text, sentence.value + " " + part.value);
                        sentence = null;
                    } else {
                        newParts.add(part);
                    }
                }
            } else {
                newParts.add(part);
            }
        }
        clear();
        addAll(newParts);
        return this;
    }

    public Message readAloud() {
        AbstractMessage newParts = new AbstractMessage();
        Iterator<MessagePart> messageParts = iterator();
        boolean readAloud = false;
        while (messageParts.hasNext()) {
            MessagePart part = messageParts.next();
            if (part.type == Type.Text) {
                String text = part.value;
                boolean readAloudStart = text.startsWith("\"");
                boolean readAloudEnd = text.endsWith("\"") || text.endsWith("\".");
                if (readAloudStart && !readAloud) {
                    newParts.add(new MessagePart(Type.Mood, Mood.Reading));
                    readAloud = true;
                } else if (readAloud) {
                    // Repeat for each text part
                    newParts.add(new MessagePart(Type.Mood, Mood.Reading));
                }
                newParts.add(part);
                if (readAloudEnd && readAloud) {
                    newParts.add(new MessagePart(Type.Mood, Mood.Neutral));
                    readAloud = false;
                }
            } else {
                newParts.add(part);
            }
        }
        this.clear();
        this.addAll(newParts);
        return this;
    }

    public List<Message> split() {
        List<Message> paragraphs = new ArrayList<>();

        Message current = new Message(actor);
        paragraphs.add(current);

        boolean header = true;

        for (MessagePart part : this) {
            boolean isHeaderType = ParagraphStart.contains(part.type);
            boolean startNewHeader = !header && isHeaderType;
            boolean headerTypeAlreadyInCurrent = current.contains(part.type) && isHeaderType;
            boolean messageComplete = current.contains(Type.Text) && isHeaderType;
            if (startNewHeader || headerTypeAlreadyInCurrent || messageComplete) {
                current = new Message(actor);
                paragraphs.add(current);
                header = true;
            } else if (header && !isHeaderType) {
                header = false;
            }
            current.add(part);
        }
        return paragraphs;
    }

    // (containsImage(currentParts) && isImage(part)
    // private boolean containsImage(MessageParts parts) {
    // return parts.contains(Type.Image) || parts.contains(new Part(Type.Keyword, Message.ActorImage))
    // || parts.contains(new Part(Type.Keyword, Message.NoImage));
    // }
    //
    // private boolean isImage(Part part) {
    // return part.type == Type.Image
    // || (part.type == Type.Keyword && (part.value == Message.ActorImage || part.value == Message.NoImage));
    // }

    public static Message join(List<Message> messages) {
        Message message = new Message(messages.get(0).actor);
        messages.stream().forEach(message::add);
        return message;
    }

    public List<String> resources() {
        List<String> resources = new ArrayList<>();
        for (MessagePart part : this) {
            boolean isImageFile = part.type == Type.Image //
                    && !NoImage.equalsIgnoreCase(part.value) //
                    && !ActorImage.equalsIgnoreCase(part.value);
            if (isImageFile || (part.type != Type.Image && Type.isFile(part.type))) {
                resources.add(part.value);
            }
        }
        return resources;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((actor == null) ? 0 : actor.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        Message other = (Message) obj;
        if (actor == null) {
            if (other.actor != null)
                return false;
        } else if (!actor.equals(other.actor))
            return false;
        return true;
    }

}
