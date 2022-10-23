package teaselib.core.media;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import teaselib.Config;
import teaselib.Message;
import teaselib.Message.Type;
import teaselib.MessagePart;
import teaselib.Mood;
import teaselib.test.ActorTestImages;
import teaselib.test.TestScript;

public class ScriptMessageDecoratorTest {

    @Test
    public void testAccumulateDelay() {
        assertEquals(new MessagePart(Type.Delay, "30.0"), ScriptMessageDecorator
                .accumulateDelay(new MessagePart(Type.Delay, "10"), new MessagePart(Type.Delay, "20.0")));

        assertEquals(new MessagePart(Type.Delay, "120.0 150.0"), ScriptMessageDecorator
                .accumulateDelay(new MessagePart(Type.Delay, "30"), new MessagePart(Type.Delay, "90.0 120")));

        assertEquals(new MessagePart(Type.Delay, "120.0 150.0"), ScriptMessageDecorator
                .accumulateDelay(new MessagePart(Type.Delay, "90 120"), new MessagePart(Type.Delay, "30.0")));

        assertEquals(new MessagePart(Type.Delay, "120.0 180.0"), ScriptMessageDecorator
                .accumulateDelay(new MessagePart(Type.Delay, "30.0 60"), new MessagePart(Type.Delay, "90 120.0")));

        assertEquals(new MessagePart(Type.Delay, "5.5 20.0"), ScriptMessageDecorator
                .accumulateDelay(new MessagePart(Type.Delay, "2 12.5"), new MessagePart(Type.Delay, "3.5 7.5")));
    }

    @Test
    public void testStandardMessage() throws IOException {
        try (TestScript script = new TestScript()) {
            script.teaseLib.config.set(Config.Render.InstructionalImages, Boolean.TRUE.toString());

            Message m = new Message(script.actor);
            m.add(Type.Image, Message.ActorImage);
            m.add(Type.Text, "FooBar");
            m.add(Type.Delay, "30.0");

            ScriptMessageDecorator scriptMessageDecorator = new ScriptMessageDecorator(script.teaseLib.config,
                    Message.ActorImage, script.actor, Mood.Neutral, script.resources, x -> x);
            RenderedMessage r = RenderedMessage.of(m, scriptMessageDecorator.all());
            assertEquals(r.toString(), 4, r.size());

            assertEquals(r.toString(), Type.Mood, r.get(0).type);
            assertEquals(r.toString(), Type.Image, r.get(1).type);
            assertEquals(r.toString(), Type.Text, r.get(2).type);
            assertEquals(r.toString(), Type.Delay, r.get(3).type);
        }
    }

    @Test
    public void testBullet() throws IOException {
        try (TestScript script = new TestScript()) {
            script.teaseLib.config.set(Config.Render.InstructionalImages, Boolean.TRUE.toString());

            Message m = new Message(script.actor);
            m.add(Type.Image, Message.ActorImage);
            m.add(Type.Item, "°");
            m.add(Type.Text, "FooBar");

            ScriptMessageDecorator scriptMessageDecorator = new ScriptMessageDecorator(script.teaseLib.config,
                    Message.ActorImage, script.actor, Mood.Neutral, script.resources, x -> x);
            RenderedMessage r = RenderedMessage.of(m, scriptMessageDecorator.all());
            assertEquals(r.toString(), 4, r.size());

            // Renders correctly -> TODO Item should be placed right before text part
            assertEquals(r.toString(), Type.Mood, r.get(0).type);
            assertEquals(r.toString(), Type.Image, r.get(1).type);
            assertEquals(r.toString(), Type.Item, r.get(2).type);
            assertEquals(r.toString(), Type.Text, r.get(3).type);
            assertEquals(r.toString(), "FooBar", r.get(3).value);
        }
    }

    @Test
    public void testImageAfterDelay() throws IOException {
        try (TestScript script = new TestScript()) {
            List<String> fetched = new ArrayList<>();
            script.actor.images = new ActorTestImages(Arrays.asList("Actor1.jpg", "Actor2.jpg"), fetched);
            script.teaseLib.config.set(Config.Render.ActorImages, Boolean.TRUE.toString());
            // TODO test should succeed without enabling instructional images
            script.teaseLib.config.set(Config.Render.InstructionalImages, Boolean.TRUE.toString());

            Message m = new Message(script.actor);
            m.add(Type.Delay, "30.0");
            m.add(Type.Image, Message.ActorImage);

            ScriptMessageDecorator scriptMessageDecorator = new ScriptMessageDecorator(script.teaseLib.config,
                    Message.ActorImage, script.actor, Mood.Neutral, script.resources, x -> x);
            RenderedMessage r = RenderedMessage.of(m, scriptMessageDecorator.all());
            assertEquals("Actor image", 4, r.size());

            assertEquals(r.toString(), Type.Mood, r.get(0).type);
            assertEquals(r.toString(), Type.Image, r.get(1).type);
            assertEquals(r.toString(), Type.Delay, r.get(2).type);
            assertEquals(r.toString(), Type.Image, r.get(3).type);

            assertEquals(2, fetched.size());
            assertEquals("Actor1.jpg", fetched.get(0));
            assertEquals("Actor2.jpg", fetched.get(1));
        }
    }

    @Test
    public void testInstructionalImage() throws IOException {
        try (TestScript script = new TestScript()) {
            List<String> fetched = new ArrayList<>();
            script.actor.images = new ActorTestImages(Arrays.asList("Actor1.jpg", "Actor2.jpg"), fetched);
            script.actor.instructions = new ActorTestImages(Arrays.asList("test.jpg"), fetched);
            script.teaseLib.config.set(Config.Render.ActorImages, Boolean.TRUE.toString());
            // TODO test should succeed without enabling instructional images
            script.teaseLib.config.set(Config.Render.InstructionalImages, Boolean.TRUE.toString());

            Message m = new Message(script.actor);
            m.add(Type.Delay, "30.0");
            m.add(Type.Image, "test.jpg");

            ScriptMessageDecorator scriptMessageDecorator = new ScriptMessageDecorator(script.teaseLib.config,
                    Message.ActorImage, script.actor, Mood.Neutral, script.resources, x -> x);
            RenderedMessage r = RenderedMessage.of(m, scriptMessageDecorator.all());
            assertEquals("Actor image", 4, r.size());

            assertEquals(r.toString(), Type.Mood, r.get(0).type);
            assertEquals(r.toString(), Type.Image, r.get(1).type);
            assertEquals(r.toString(), Type.Delay, r.get(2).type);
            assertEquals(r.toString(), Type.Image, r.get(3).type);

            assertEquals("Actor1.jpg", fetched.get(0));
            assertEquals("test.jpg", fetched.get(1));
        }
    }

}
