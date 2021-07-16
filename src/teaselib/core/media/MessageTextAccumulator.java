package teaselib.core.media;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import teaselib.Message;
import teaselib.MessagePart;

public class MessageTextAccumulator {
    private static final String PARAGRAPH_SEPARATOR = "\n\n";
    List<String> paragraphs;
    private String tail;

    boolean appendToParagraph = false;

    public MessageTextAccumulator() {
        this.paragraphs = new ArrayList<>();
        this.tail = "";
    }

    public final void add(MessagePart part) {
        StringBuilder tailBuilder;
        if (part.type == Message.Type.Text) {
            add(part.value);
        } else if (part.type == Message.Type.Item) {
            tailBuilder = new StringBuilder();
            tailBuilder.append("° ");
            tailBuilder.append(removeKeyword(part));
            tail = tailBuilder.toString();
            paragraphs.add(tail);
            appendToParagraph = true;
        }
    }

    public final void add(String text) {
        StringBuilder tailBuilder;
        if (appendToParagraph) {
            tailBuilder = new StringBuilder(removeLast());
            tailBuilder.append(" ");
        } else {
            tailBuilder = new StringBuilder();
        }
        tailBuilder.append(text);
        tail = tailBuilder.toString();
        paragraphs.add(tail);
        appendToParagraph = canAppendTo(tail);
    }

    private String removeLast() {
        return paragraphs.remove(paragraphs.size() - 1);
    }

    private static String removeKeyword(MessagePart part) {
        String value = part.value;
        if (part.type == Message.Type.Item && value.startsWith(Message.Bullet)) {
            return value.substring(Message.Bullet.length()).trim();
        } else if (value.equalsIgnoreCase(part.type.toString())) {
            return "";
        } else {
            return part.value.substring(part.type.toString().length() + 1);
        }
    }

    public static boolean canAppendTo(String string) {
        String ending = string.isEmpty() ? " " : string.substring(string.length() - 1, string.length());
        return Message.MainClauseAppendableCharacters.contains(ending);
    }

    public boolean canAppend() {
        return appendToParagraph;
    }

    public String getTail() {
        return tail;
    }

    @Override
    public String toString() {
        return paragraphs.stream().collect(Collectors.joining(PARAGRAPH_SEPARATOR));
    }

}
