package teaselib;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import teaselib.Message.Part;
import teaselib.Message.Type;

public class MessageParts implements Iterable<Part> {

    private final List<Part> p = new ArrayList<>();

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

    public void addAll(MessageParts parts) {
        for (Part part : parts) {
            add(part);
        }
    }

    @Override
    public Iterator<Part> iterator() {
        return p.iterator();
    }

    public Stream<Part> stream() {
        return p.stream();
    }

    void add(Part part) {
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
        Part part = new Part(text);
        add(part);
    }

    public void add(Type type, String value) {
        add(new Part(type, value));
    }

    public int size() {
        return p.size();
    }

    public boolean contains(Type type) {
        for (Part part : p) {
            if (part.type == type) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(Part part) {
        return p.contains(part);
    }

    public Part get(int index) {
        return p.get(index);
    }

    @Override
    public String toString() {
        return "size=" + size() + ", " + p.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((p == null) ? 0 : p.hashCode());
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
        MessageParts other = (MessageParts) obj;
        if (p == null) {
            if (other.p != null)
                return false;
        } else if (!p.equals(other.p))
            return false;
        return true;
    }
}