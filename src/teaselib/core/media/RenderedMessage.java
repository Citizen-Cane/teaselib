package teaselib.core.media;

import java.util.Arrays;
import java.util.List;

import teaselib.Message;

public class RenderedMessage extends Message {
    @FunctionalInterface
    public interface Function {
        Message process(Message message);
    }

    RenderedMessage(Message message, List<Function> functions) {
        super(message.actor);

        for (Function function : functions) {
            message = function.process(message);
        }

        addAll(message);
    }

    public static RenderedMessage of(Message message, Function... renderFunctions) {
        return new RenderedMessage(message, Arrays.asList(renderFunctions));
    }
}
