package teaselib.core.media;

import java.util.Arrays;
import java.util.List;

import teaselib.AbstractMessage;
import teaselib.Message;
import teaselib.MessagePart;

public class RenderedMessage extends Message {
    @FunctionalInterface
    public interface Decorator {
        Message process(Message message);
    }

    RenderedMessage(Message message, List<Decorator> decorators) {
        super(message.actor);

        for (Decorator decorator : decorators) {
            message = decorator.process(message);
        }

        addAll(message);
    }

    public static RenderedMessage of(Message message, Decorator... renderFunctions) {
        return new RenderedMessage(message, Arrays.asList(renderFunctions));
    }

    public static Message getLastSection(Message message) {
        int index = findLastTextElement(message);

        if (index < 0) {
            return message;
        }

        index = findStartOfHeader(message, index);
        return copyTextHeader(message, index);
    }

    static int findStartOfHeader(Message message, int index) {
        AbstractMessage parts = message;
        while (index-- > 0) {
            Type type = parts.get(index).type;
            if (type == Message.Type.Text || type == Message.Type.Speech || type == Message.Type.Sound
                    || type == Message.Type.Delay) {
                // last section starts after the second last text part
                index++;
                break;
            }
        }
        return index;
    }

    static int findLastTextElement(Message message) {
        AbstractMessage parts = message;
        int index = parts.size();
        while (index-- > 0) {
            Type type = parts.get(index).type;
            if (type == Message.Type.Text) {
                break;
            }
        }
        return index;
    }

    static Message copyTextHeader(Message message, int index) {
        if (index < 0) {
            index = 0;
        }

        Message lastSection = new Message(message.actor);
        boolean afterText = false;
        AbstractMessage parts = message;
        for (int i = index; i < parts.size(); i++) {
            MessagePart part = parts.get(i);
            if (part.type == Message.Type.DesktopItem) {
                // skip
            } else if (part.type == Message.Type.Delay && !afterText) {
                // skip
            } else {
                lastSection.add(part);
            }
            if (part.type == Message.Type.Text) {
                afterText = true;
            }
        }
        return lastSection;
    }
}
