package teaselib.core;

import static org.junit.Assert.*;

import org.junit.Test;

import teaselib.Message;
import teaselib.Message.Parts;
import teaselib.Message.Type;
import teaselib.test.TestScript;

public class TeaseScriptBaseTest {
    @Test
    public void testEmptyMessage() {
        TestScript script = TestScript.getOne(getClass());

        Message message = new Message(script.actor);
        script.setImage("foobar.jpg");
        Message parsed = script.injectImagesAndExpandTextVariables(message);
        Parts parts = parsed.getParts();
        int n = 0;
        assertEquals(Type.Image, parts.get(n).type);

        assertEquals(parts.size(), 1);
    }

    @Test
    public void testInjectionOfInlineResources() {
        TestScript script = TestScript.getOne(getClass());

        Message message = new Message(script.actor);
        message.add("Some text.");
        message.add("foobar.jpg");
        message.add("Some more text.");
        message.add(Message.ActorImage);
        message.add("Even more text.");
        message.add("foo.jpg");
        message.add(Type.Delay, "2");
        message.add("bar.jpg");

        Message parsed = script.injectImagesAndExpandTextVariables(message);
        Parts parts = parsed.getParts();
        int n = 0;
        assertEquals(Type.Mood, parts.get(n++).type);
        assertEquals(Type.Image, parts.get(n).type);
        assertEquals(Message.NoImage, parts.get(n++).value);
        assertEquals(Type.Text, parts.get(n++).type);

        assertEquals(Type.Image, parts.get(n).type);
        assertEquals("foobar.jpg", parts.get(n++).value);
        assertEquals(Type.Mood, parts.get(n++).type);
        assertEquals(Type.Text, parts.get(n++).type);

        assertEquals(Type.Mood, parts.get(n++).type);
        assertEquals(Type.Image, parts.get(n++).type);
        assertEquals(Type.Text, parts.get(n++).type);

        assertEquals(Type.Image, parts.get(n).type);
        assertEquals("foo.jpg", parts.get(n++).value);
        assertEquals(Type.Delay, parts.get(n++).type);

        assertEquals(Type.Image, parts.get(n).type);
        assertEquals("bar.jpg", parts.get(n++).value);

        assertEquals(parts.size(), n);
    }

    @Test
    public void testInjectionOfScriptImage() {
        TestScript script = TestScript.getOne(getClass());

        Message message = new Message(script.actor);
        message.add("Some text.");
        script.setImage("foo.jpg");

        Message parsed = script.injectImagesAndExpandTextVariables(message);
        Parts parts = parsed.getParts();
        int n = 0;
        assertEquals(Type.Mood, parts.get(n++).type);
        assertEquals(Type.Image, parts.get(n).type);
        assertEquals("foo.jpg", parts.get(n++).value);
        assertEquals(Type.Text, parts.get(n++).type);

        assertEquals(parts.size(), n);
    }

}
