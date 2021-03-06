package teaselib.core.media;

import teaselib.Message;
import teaselib.MessagePart;

public class MessageTextAccumulator {
    private final StringBuilder message;
    boolean appendToParagraph = true;

    public MessageTextAccumulator() {
        message = new StringBuilder();
    }

    public final void add(MessagePart part) {
        if (part.type == Message.Type.Text) {
            if (!appendToParagraph) {
                newParagraph();
            } else {
                message.append(" ");
            }
            message.append(part.value);
            appendToParagraph = canAppendToParagraph();
        } else if (part.type == Message.Type.Item) {
            newParagraph();
            message.append("� ");
            message.append(removeKeyword(part));
            appendToParagraph = true;
        }
    }

    protected void newParagraph() {
        if (message.length() > 0) {
            message.append("\n\n");
        }
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

    private boolean canAppendToParagraph() {
        return canAppendTo(message.toString());
    }

    public static boolean canAppendTo(String string) {
        String ending = string.isEmpty() ? " " : string.substring(string.length() - 1, string.length());
        return Message.MainClauseAppendableCharacters.contains(ending);
    }

    public boolean canAppend() {
        return appendToParagraph;
    }

    @Override
    public String toString() {
        return message.toString();
    }

}
