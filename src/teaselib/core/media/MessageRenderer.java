package teaselib.core.media;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Message;
import teaselib.core.TeaseLib;
import teaselib.util.AnnotatedImage;

public abstract class MessageRenderer extends MediaRendererThread implements ReplayableMediaRenderer {

    static final Logger logger = LoggerFactory.getLogger(MessageRenderer.class);

    static final Set<Message.Type> ManuallyLoggedMessageTypes = new HashSet<>(Arrays.asList(Message.Type.Text,
            Message.Type.Image, Message.Type.Mood, Message.Type.Sound, Message.Type.Speech, Message.Type.Delay));

    // TODO blocks multiple actors in one batch
    final List<RenderedMessage> messages;
    RenderedMessage lastParagraph;

    MessageTextAccumulator accumulatedText = new MessageTextAccumulator();
    String displayImageName = null;
    AnnotatedImage displayImage = null;

    protected MessageRenderer(TeaseLib teaseLib, List<RenderedMessage> messages) {
        super(teaseLib);
        this.messages = new ArrayList<>(messages);
        this.lastParagraph = lastParagraph();
    }

    RenderedMessage lastParagraph() {
        if (messages.isEmpty()) {
            return new RenderedMessage();
        } else {
            return RenderedMessage.getLastParagraph(getLastMessage());
        }
    }

    private RenderedMessage getLastMessage() {
        return getLastMessage(this.messages);
    }

    private static RenderedMessage getLastMessage(List<RenderedMessage> messages) {
        return messages.get(messages.size() - 1);
    }

    public abstract boolean append(List<RenderedMessage> newMessages);

    public abstract void forwardToEnd();

    @Override
    public String toString() {
        return messages.toString();
    }

}
