package teaselib.core.media;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import teaselib.Message;
import teaselib.Message.Type;
import teaselib.MessagePart;

public class RenderedMessageTest {

    @Test
    public void testLastSection() {
        Message message = new Message(null, "Foo.jpg", "Foo.", "Bar.jpg", "Bar.");

        int findLastTextElement = RenderedMessage.findLastTextElement(message);
        assertEquals(3, findLastTextElement);
        int findStartOfHeader = RenderedMessage.findStartOfHeader(message, findLastTextElement);
        assertEquals(2, findStartOfHeader);

        assertEquals(2, RenderedMessage.getLastParagraph(message).size());
    }

    @Test
    public void testLastSectionWithSpeech() {
        Message message = new Message(null);
        message.add(new MessagePart(Type.Image, "Foo.jpg"));
        message.add(new MessagePart(Type.Text, "Foo."));
        message.add(new MessagePart(Type.Speech, "Foo."));
        message.add(new MessagePart(Type.Image, "Bar.jpg"));
        message.add(new MessagePart(Type.Text, "Bar."));
        message.add(new MessagePart(Type.Speech, "Bar."));

        int findLastTextElement = RenderedMessage.findLastTextElement(message);
        assertEquals(4, findLastTextElement);
        int findStartOfHeader = RenderedMessage.findStartOfHeader(message, findLastTextElement);
        assertEquals(3, findStartOfHeader);

        assertEquals(3, RenderedMessage.getLastParagraph(message).size());
    }

}
