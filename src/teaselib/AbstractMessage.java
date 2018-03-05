package teaselib;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import teaselib.Message.Type;

public class AbstractMessage implements Iterable<MessagePart> {
    public final Actor actor;
    private final List<MessagePart> parts = new ArrayList<>();

    public AbstractMessage(Actor actor) {
        this.actor = actor;
    }

    public AbstractMessage(Actor actor, List<MessagePart> parts) {
        this.actor = actor;
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
            throw new IllegalArgumentException(text);
        MessagePart part = new MessagePart(text);
        add(part);
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

    public boolean contains(MessagePart part) {
        return parts.contains(part);
    }

    public MessagePart get(int index) {
        return parts.get(index);
    }

    @Override
    public String toString() {
        return "size=" + size() + ", " + parts.toString();
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
        AbstractMessage other = (AbstractMessage) obj;
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
}