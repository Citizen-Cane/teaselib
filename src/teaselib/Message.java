package teaselib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Message {
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

    public static final String Bullet = "�";

    public static final String[] Keywords = { Delay, ShowOnDesktop, ShowChoices, AwaitSoundCompletion, Bullet };

    public final Actor actor;

    public static final Set<String> EndOfSentenceCharacters = new HashSet<>(Arrays.asList(":", ".", "!", "?"));
    public static final Set<String> MainClauseAppendableCharacters = new HashSet<>(
            Arrays.asList("\"", ">", ",", ";", "-"));

    private static final Set<Type> ParagraphStart = new HashSet<>(
            Arrays.asList(Type.Image, Type.Mood, Type.Text, Type.Speech));

    private static final Set<String> ImageKeywords = new HashSet<>(
            Arrays.asList(ActorImage.toLowerCase(), NoImage.toLowerCase()));

    private final MessageParts parts;

    public Message(Actor actor) {
        this.parts = new MessageParts();
        this.actor = actor;
    }

    /**
     * @param message
     *            The message to render, or null or an empty list to display no message
     */
    public Message(Actor actor, String... message) {
        parts = new MessageParts();
        if (message != null) {
            parts.addAll(message);
        }
        this.actor = actor;
    }

    /**
     * @param message
     *            The message to render, or null or an empty list to display no message
     */
    public Message(Actor actor, List<String> message) {
        this.parts = new MessageParts();
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
     * Use with caution, since parts are not concatenated to sentences, which may result in collisions when prerecording
     * speech
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

    /**
     * Builds a string with all the formatting.
     * 
     * @return
     */
    public String toText() {
        return buildString("\n\n", true);
    }

    /**
     * Converts the message to a hash string suitable for speech pre-recording. The string contains only the message
     * parts that are relevant for pre-rendering speech - all other media hints are removed.
     * 
     * @return
     */
    public String toPrerecordedSpeechHashString() {
        return buildString("\n", false);
    }

    public String buildString(String newLine, boolean all) {
        if (parts == null) {
            return "";
        } else if (parts.isEmpty()) {
            return "";
        } else {
            StringBuilder messageString = new StringBuilder();
            for (Iterator<Part> i = parts.iterator(); i.hasNext();) {
                Part part = i.next();
                boolean appendPart = all || part.type == Type.Text || part.type == Type.Mood;
                if (appendPart) {
                    if (messageString.length() > 0) {
                        messageString.append(newLine);
                    }
                    if (part.type == Type.Text || part.type == Type.Mood) {
                        messageString.append(part.value);
                    } else {
                        messageString.append(part.toString());
                    }
                }
            }
            return messageString.toString();
        }
    }

    public MessageParts getParts() {
        return parts;
    }

    public static boolean isFile(String m) {
        int s = m.length();
        int i = m.lastIndexOf(".");

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
                return m.substring(i, s - 1).indexOf(" ") < 0;
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

    public static class Part {
        public final Type type;
        public final String value;

        public Part(String text) {
            text = text.trim();
            Type type = determineType(text);
            if (type == Type.Keyword) {
                text = keywordFrom(text);
            } else if (type == Type.DesktopItem) {
                // The keyword is optional,
                // it's just needed to show an image on the desktop
                text = removeAnyKeywordInFront(ShowOnDesktop, text);
            } else if (type == Type.Delay) {
                text = removeAnyKeywordInFront(Delay, text);
            }
            this.type = type;
            this.value = text;
        }

        private static String removeAnyKeywordInFront(String keyword, String text) {
            if (text.toLowerCase().startsWith(keyword)) {
                text = text.substring(keyword.length()).trim();
            }
            return text;
        }

        public Part(Type type, String value) {
            this.type = type;
            this.value = value;
        }

        public boolean isFile() {
            return Message.Type.isFile(type);
        }

        @Override
        public String toString() {
            return type.name() + "=" + value;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Part other = (Part) obj;
            if (type != other.type)
                return false;
            if (value == null) {
                if (other.value != null)
                    return false;
            } else if (!value.equals(other.value))
                return false;
            return true;
        }
    }

    public Message joinSentences() {
        MessageParts newParts = new MessageParts();
        Iterator<Part> parts = this.parts.iterator();
        Part sentence = null;
        while (parts.hasNext()) {
            Part part = parts.next();
            if (part.type == Type.Text) {
                if (!endOf(part.value, EndOfSentenceCharacters) && !endOf(part.value, MainClauseAppendableCharacters)) {
                    if (sentence == null) {
                        sentence = part;
                    } else {
                        sentence = new Part(Type.Text, sentence.value + " " + part.value);
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
        this.parts.clear();
        this.parts.addAll(newParts);
        return this;
    }

    public Message readAloud() {
        MessageParts newParts = new MessageParts();
        Iterator<Part> parts = this.parts.iterator();
        boolean readAloud = false;
        while (parts.hasNext()) {
            Part part = parts.next();
            if (part.type == Type.Text) {
                String text = part.value;
                boolean readAloudStart = text.startsWith("\"");
                boolean readAloudEnd = text.endsWith("\"") || text.endsWith("\"."); // todo
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

    public List<Message> split() {
        List<Message> paragraphs = new ArrayList<>();

        Message current = new Message(actor);
        paragraphs.add(current);

        boolean header = true;

        for (Part part : parts) {
            boolean isHeaderType = ParagraphStart.contains(part.type);
            boolean startNewHeader = !header && isHeaderType;
            boolean headerTypeAlreadyInCurrent = current.getParts().contains(part.type) && isHeaderType;
            boolean messageComplete = current.getParts().contains(Type.Text) && isHeaderType;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((actor == null) ? 0 : actor.hashCode());
        result = prime * result + ((parts == null) ? 0 : parts.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Message other = (Message) obj;
        if (actor == null) {
            if (other.actor != null)
                return false;
        } else if (!actor.equals(other.actor))
            return false;
        if (parts == null) {
            if (other.parts != null)
                return false;
        } else if (!parts.equals(other.parts))
            return false;
        return true;
    }

    public List<String> resources() {
        List<String> resources = new ArrayList<>();
        for (Part part : parts) {
            boolean isImageFile = part.type == Type.Image //
                    && NoImage.equalsIgnoreCase(part.value) //
                    && ActorImage.equalsIgnoreCase(part.value);
            if (isImageFile && Type.isFile(part.type)) {
                resources.add(part.value);
            }
        }
        return resources;
    }
}
