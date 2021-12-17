package teaselib.core.media;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import teaselib.Message;
import teaselib.MessagePart;

class MessageTextAccumulator {

    private static final String PARAGRAPH_SEPARATOR = "\n\n";

    final List<String> paragraphs;

    private String tail;
    private boolean appendToParagraph = false;

    MessageTextAccumulator() {
        this.paragraphs = new ArrayList<>();
        this.tail = "";
    }

    void add(MessagePart part) {
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

    void add(String text) {
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

    void addAll(List<RenderedMessage> messages) {
        messages.stream().flatMap(RenderedMessage::stream).filter(part -> Message.Type.TextTypes.contains(part.type))
                .map(p -> p.value).forEach(this::add);
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

    static boolean canAppendTo(String string) {
        String ending = string.isEmpty() ? " " : string.substring(string.length() - 1, string.length());
        return Message.MainClauseAppendableCharacters.contains(ending);
    }

    String getTail() {
        return tail;
    }

    @Override
    public String toString() {
        return paragraphs.stream().collect(Collectors.joining(PARAGRAPH_SEPARATOR));
    }

}
