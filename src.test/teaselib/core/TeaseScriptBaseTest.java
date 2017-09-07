package teaselib.core;

import static org.junit.Assert.*;

import org.junit.Test;

import teaselib.Message;
import teaselib.Message.Parts;
import teaselib.Message.Type;
import teaselib.Mood;
import teaselib.test.DebugSetup;
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
        TestScript script = TestScript.getOne(new DebugSetup().withOutput());

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
        assertEquals(Type.Text, parts.get(n++).type);

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
    public void testInjectionOfMood() {
        TestScript script = TestScript.getOne(new DebugSetup().withOutput());

        script.setMood(Mood.Friendly);

        Message message = new Message(script.actor);
        message.add(Mood.Harsh);
        message.add("I'm harsh.");

        message.add("foobar.jpg");
        message.add("I'm friendly.");

        message.add(Message.ActorImage);
        message.add(Mood.Strict);
        message.add("I'm strict.");

        message.add("foo.jpg");
        message.add(Type.Delay, "2");

        message.add("bar.jpg");
        message.add("I'm friendly again.");

        message.add("I'm still friendly.");

        message.add(Mood.Happy);
        message.add("I'm happy.");

        Message parsed = script.injectImagesAndExpandTextVariables(message);
        Parts parts = parsed.getParts();
        int n = 0;

        assertEquals(Type.Mood, parts.get(n).type);
        assertEquals(Mood.Harsh, parts.get(n++).value);
        assertEquals(Type.Image, parts.get(n).type);
        assertEquals(Message.NoImage, parts.get(n++).value);
        assertEquals(Type.Text, parts.get(n++).type);

        assertEquals(Type.Mood, parts.get(n).type);
        assertEquals(Mood.Friendly, parts.get(n++).value);
        assertEquals(Type.Image, parts.get(n).type);
        assertEquals("foobar.jpg", parts.get(n++).value);
        assertEquals(Type.Text, parts.get(n++).type);

        assertEquals(Type.Mood, parts.get(n).type);
        assertEquals(Mood.Strict, parts.get(n++).value);
        assertEquals(Type.Image, parts.get(n++).type);
        assertEquals(Type.Text, parts.get(n++).type);

        assertEquals(Type.Mood, parts.get(n).type);
        assertEquals(Mood.Friendly, parts.get(n++).value);
        assertEquals(Type.Image, parts.get(n).type);
        assertEquals("foo.jpg", parts.get(n++).value);
        assertEquals(Type.Delay, parts.get(n++).type);

        assertEquals(Type.Image, parts.get(n++).type);
        assertEquals(Type.Text, parts.get(n++).type);

        assertEquals(Type.Text, parts.get(n++).type);

        assertEquals(Type.Mood, parts.get(n).type);
        assertEquals(Mood.Happy, parts.get(n++).value);
        assertEquals(Type.Text, parts.get(n++).type);

        assertEquals(parts.size(), n);
    }

    @Test
    public void testInjectionOfScriptImage() {
        TestScript script = TestScript.getOne(new DebugSetup().withOutput());

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

    @Test
    public void testInjectionOfNoImage() {
        TestScript script = TestScript.getOne(new DebugSetup());

        Message message = new Message(script.actor);
        message.add("Some text.");
        script.setImage("foo.jpg");

        Message parsed = script.injectImagesAndExpandTextVariables(message);
        Parts parts = parsed.getParts();
        int n = 0;
        assertEquals(Type.Mood, parts.get(n++).type);
        assertEquals(Type.Image, parts.get(n).type);
        assertEquals(Message.NoImage, parts.get(n++).value);
        assertEquals(Type.Text, parts.get(n++).type);

        assertEquals(parts.size(), n);
    }

}
