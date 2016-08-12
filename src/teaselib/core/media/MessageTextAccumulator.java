package teaselib.core.media;

import teaselib.Message;
import teaselib.Message.Part;

public class MessageTextAccumulator {
    private final StringBuilder message;
    boolean appendToParagraph = true;

    public MessageTextAccumulator() {
        message = new StringBuilder();
    }

    public final void add(Message.Part part) {
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
            message.append("° ");
            message.append(removeKeyword(part));
            appendToParagraph = true;
        }
    }

    protected void newParagraph() {
        if (message.length() > 0) {
            message.append("\n\n");
        }
    }

    private static String removeKeyword(Part part) {
        String value = part.value;
        if (part.type == Message.Type.Item
                && value.startsWith(Message.Bullet)) {
            return value.substring(Message.Bullet.length()).trim();
        } else if (value.equalsIgnoreCase(part.type.toString())) {
            return "";
        } else {
            return part.value.substring(part.type.toString().length() + 1);
        }
    }

    private boolean canAppendToParagraph() {
        String s = message.toString();
        String ending = s.isEmpty() ? " "
                : s.substring(s.length() - 1, s.length());
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
