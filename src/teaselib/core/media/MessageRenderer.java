package teaselib.core.media;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Message;
import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;

public abstract class MessageRenderer extends MediaRendererThread implements ReplayableMediaRenderer {

    static final Logger logger = LoggerFactory.getLogger(MessageRenderer.class);

    static final Set<Message.Type> ManuallyLoggedMessageTypes = new HashSet<>(Arrays.asList(Message.Type.Text,
            Message.Type.Image, Message.Type.Mood, Message.Type.Sound, Message.Type.Speech, Message.Type.Delay));

    // TODO blocks multiple actors in one batch
    final Actor actor;
    final ResourceLoader resources;
    final List<RenderedMessage> messages;

    MessageTextAccumulator accumulatedText = new MessageTextAccumulator();

    int currentMessage = 0;
    String displayImage = null;
    RenderedMessage previousLastParagraph;
    RenderedMessage lastParagraph;

    protected MessageRenderer(TeaseLib teaseLib, Actor actor, ResourceLoader resources,
            List<RenderedMessage> messages) {
        super(teaseLib);
        this.actor = actor;
        this.resources = resources;
        this.messages = new ArrayList<>(messages);
        this.lastParagraph = RenderedMessage.getLastParagraph(getLastMessage());
    }

    RenderedMessage getLastMessage() {
        return getLastMessage(this.messages);
    }

    private static RenderedMessage getLastMessage(List<RenderedMessage> messages) {
        return messages.get(messages.size() - 1);
    }

    RenderedMessage getEnd() {
        return RenderedMessage.getLastParagraph(getLastMessage()).stripAudio();
    }

    @Override
    public String toString() {
        return messages.toString();
    }

}
