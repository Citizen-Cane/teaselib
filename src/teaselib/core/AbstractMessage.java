package teaselib.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import teaselib.Message.Type;
import teaselib.MessagePart;

public class AbstractMessage implements Iterable<MessagePart> {
    private final List<MessagePart> parts = new ArrayList<>();

    public AbstractMessage() {
    }

    public AbstractMessage(List<MessagePart> parts) {
        this.parts.addAll(parts);
    }

    public boolean isEmpty() {
        return parts.isEmpty();
    }

    public void clear() {
        parts.clear();
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

    public void addAll(AbstractMessage message) {
        for (MessagePart part : message) {
            add(part);
        }
    }

    public void add(AbstractMessage message) {
        if (message == null)
            throw new IllegalArgumentException();
        for (MessagePart part : message) {
            add(part);
        }
    }

    @Override
    public Iterator<MessagePart> iterator() {
        return parts.iterator();
    }

    public Stream<MessagePart> stream() {
        return parts.stream();
    }

    /**
     * Use with caution, since parts are not concatenated to sentences, which may result in collisions when prerecording
     * speech
     * 
     * @param part
     */
    public void add(MessagePart part) {
        if (part.type == Type.Text) {
            parts.add(part);
        } else if (part.type == Type.Mood) {
            if (!parts.isEmpty()) {
                int i = parts.size() - 1;
                MessagePart previous = parts.get(i);
                if (previous.type == Type.Mood) {
                    parts.set(i, part);
                } else {
                    parts.add(part);
                }
            } else {
                parts.add(part);
            }
        } else {
            parts.add(part);
        }
    }

    public void add(String text) {
        if (text == null)
            text = "";
        MessagePart part = new MessagePart(text);
        add(part);
    }

    public void add(Type type, double value) {
        add(type, Double.toString(value));
    }

    public void add(Type type, int value) {
        add(type, Integer.toString(value));
    }

    public void add(Type type, String value) {
        add(new MessagePart(type, value));
    }

    public int size() {
        return parts.size();
    }

    public boolean contains(Type type) {
        for (MessagePart part : parts) {
            if (part.type == type) {
                return true;
            }
        }
        return false;
    }

    public MessagePart find(Type type) {
        for (MessagePart part : parts) {
            if (part.type == type) {
                return part;
            }
        }
        return null;
    }

    public MessagePart findLast(Type type) {
        for (int i = parts.size() - 1; i >= 0; --i) {
            var part = parts.get(i);
            if (part.type == type) {
                return part;
            }
        }
        return null;
    }

    public boolean contains(MessagePart part) {
        return parts.contains(part);
    }

    public MessagePart get(int index) {
        return parts.get(index);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + parts.hashCode();
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
        AbstractMessage other = (AbstractMessage) obj;
        return parts.equals(other.parts);
    }

    @Override
    public String toString() {
        return buildString("\n", true);
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
        if (isEmpty()) {
            return "";
        } else {
            StringBuilder messageString = new StringBuilder();
            for (Iterator<MessagePart> i = iterator(); i.hasNext();) {
                MessagePart part = i.next();
                appendPart(messageString, part, newLine, all);
            }
            return messageString.toString();
        }
    }

    private static void appendPart(StringBuilder messageString, MessagePart part, String newLine, boolean all) {
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

}