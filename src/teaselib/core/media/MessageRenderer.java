package teaselib.core.media;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Images;
import teaselib.Message;
import teaselib.MessagePart;
import teaselib.core.AbstractImages;
import teaselib.core.AbstractMessage;
import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;

public abstract class MessageRenderer extends MediaRendererThread implements Runnable, ReplayableMediaRenderer {

    static final Logger logger = LoggerFactory.getLogger(MessageRenderer.class);

    static final Set<Message.Type> ManuallyLoggedMessageTypes = new HashSet<>(Arrays.asList(Message.Type.Text,
            Message.Type.Image, Message.Type.Mood, Message.Type.Sound, Message.Type.Speech, Message.Type.Delay));

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
        this.messages = messages;
        this.lastParagraph = RenderedMessage.getLastParagraph(getLastMessage());
        prefetchImages();
    }

    void prefetchImages() {
        prefetchImages(actor, messages);
    }

    private static void prefetchImages(Actor actor, List<RenderedMessage> messages) {
        if (actor.images != Images.None) {
            for (RenderedMessage message : messages) {
                prefetchImages(actor, message);
            }
            if (actor.images instanceof AbstractImages) {
                ((AbstractImages) actor.images).prefetcher().fetch();
            }
        }
    }

    private static void prefetchImages(Actor actor, RenderedMessage message) {
        for (MessagePart part : message) {
            if (part.type == Message.Type.Image) {
                String resource = part.value;
                if (!Message.NoImage.equals(part.value) && actor.images instanceof AbstractImages) {
                    ((AbstractImages) actor.images).prefetcher().add(resource);
                }
            }
        }
    }

    private RenderedMessage getLastMessage() {
        return getLastMessage(this.messages);
    }

    private static RenderedMessage getLastMessage(List<RenderedMessage> messages) {
        return messages.get(messages.size() - 1);
    }

    RenderedMessage getEnd() {
        return stripAudio(RenderedMessage.getLastParagraph(getLastMessage()));
    }

    private static RenderedMessage stripAudio(AbstractMessage message) {
        return message.stream().filter(part -> !Message.Type.AudioTypes.contains(part.type))
                .collect(RenderedMessage.collector());
    }

    public abstract void play() throws IOException, InterruptedException;

    @Override
    public String toString() {
        return messages.toString();
    }

}
