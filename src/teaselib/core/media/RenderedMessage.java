package teaselib.core.media;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collector;

import teaselib.Message;
import teaselib.MessagePart;
import teaselib.core.AbstractMessage;

public class RenderedMessage extends AbstractMessage {
    @FunctionalInterface
    public interface Decorator {
        AbstractMessage process(AbstractMessage message);
    }

    RenderedMessage(Message message, List<Decorator> decorators) {
        AbstractMessage decorated = message;
        for (Decorator decorator : decorators) {
            decorated = decorator.process(decorated);
        }

        addAll(decorated);
    }

    RenderedMessage() {
    }

    public static RenderedMessage of(Message message, Decorator... renderFunctions) {
        return new RenderedMessage(message, Arrays.asList(renderFunctions));
    }

    public static Collector<MessagePart, RenderedMessage, RenderedMessage> collector() {
        return Collector.of(RenderedMessage::new, //
                (message, part) -> message.add(part), //
                (message1, message2) -> {
                    message1.add(message2);
                    return message1;
                }, Collector.Characteristics.UNORDERED);
    }

    public RenderedMessage getLastSection() {
        return getLastSection();
    }

    public static RenderedMessage getLastSection(AbstractMessage message) {
        int index = findLastTextElement(message);

        if (index < 0) {
            RenderedMessage renderedMessage = new RenderedMessage();
            renderedMessage.addAll(message);
            return renderedMessage;
        }

        index = findStartOfHeader(message, index);
        return copyTextHeader(message, index);
    }

    static int findStartOfHeader(AbstractMessage message, int index) {
        AbstractMessage parts = message;
        while (index-- > 0) {
            Message.Type type = parts.get(index).type;
            if (type == Message.Type.Text || type == Message.Type.Speech || type == Message.Type.Sound
                    || type == Message.Type.Delay) {
                // last section starts after the second last text part
                index++;
                break;
            }
        }
        return index;
    }

    static int findLastTextElement(AbstractMessage message) {
        AbstractMessage parts = message;
        int index = parts.size();
        while (index-- > 0) {
            Message.Type type = parts.get(index).type;
            if (type == Message.Type.Text) {
                break;
            }
        }
        return index;
    }

    static RenderedMessage copyTextHeader(AbstractMessage message, int index) {
        if (index < 0) {
            index = 0;
        }

        RenderedMessage lastSection = new RenderedMessage();
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
