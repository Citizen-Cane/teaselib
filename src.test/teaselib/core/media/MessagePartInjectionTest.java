package teaselib.core.media;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Actor;
import teaselib.Images;
import teaselib.Message;
import teaselib.Message.Type;
import teaselib.Mood;
import teaselib.core.AbstractMessage;
import teaselib.core.Configuration.Setup;
import teaselib.test.DebugSetup;
import teaselib.test.TestScript;
import teaselib.util.RandomImages;

public class MessagePartInjectionTest {
    private final class DecoratingTestScript extends TestScript {

        public DecoratingTestScript() throws IOException {
            super();
        }

        public DecoratingTestScript(Setup setup) throws IOException {
            super(setup);
        }

        public RenderedMessage.Decorator[] getDecorators() {
            return new ScriptMessageDecorator(teaseLib.config, displayImage, actor, mood, resources,
                    this::expandTextVariables, Optional.empty()).messageModifiers();
        }

        public String expandTextVariables(String text) {
            return text;
        }

        public void renderMessage(Message message) {
            super.renderMessage(message, false);
        }
    }

    public final class ActorTestImage implements Images {
        private final String resourcePath;

        public ActorTestImage(String resourcePath) {
            this.resourcePath = resourcePath;
        }

        @Override
        public String next() {
            return resourcePath;
        }

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public void hint(String... hint) {
        }

        @Override
        public boolean contains(String resource) {
            return resourcePath.equals(resource);
        }
    }

    static final Actor DummyActor = null;

    private RenderedMessage decorate(DecoratingTestScript script, Message message) {
        return RenderedMessage.of(message, script.getDecorators());
    }

    @Test
    public void testEmptyMessage() throws IOException {
        DecoratingTestScript script = new DecoratingTestScript();
        Message message = new Message(script.actor);
        script.setImage("foobar.jpg");
        RenderedMessage parsed = decorate(script, message);
        int n = 0;

        assertEquals(Type.Image, parsed.get(n).type);

        assertEquals(1, parsed.size());
    }

    @Test
    public void testInjectionOfInlineResources() throws IOException {
        DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withOutput());
        script.actor.images = new ActorTestImage("Actor.jpg");

        Message message = new Message(script.actor);
        message.add("Some text.");
        message.add("foobar.jpg");
        message.add("Some more text.");
        message.add(Message.ActorImage);
        message.add("Even more text.");
        message.add("foo.jpg");
        message.add(Type.Delay, "2");
        message.add("bar.jpg");

        RenderedMessage parsed = decorate(script, message);
        int n = 0;

        assertEquals(Type.Mood, parsed.get(n++).type);
        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("Actor.jpg", parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);

        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("foobar.jpg", parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);

        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("Actor.jpg", parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);

        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("foo.jpg", parsed.get(n++).value);
        assertEquals(Type.Delay, parsed.get(n++).type);

        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("bar.jpg", parsed.get(n++).value);

        assertEquals(parsed.size(), n);
    }

    @Test
    public void testInjectionOfMood() throws IOException {
        DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withOutput());
        script.actor.images = new ActorTestImage("Actor.jpg");

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

        RenderedMessage parsed = decorate(script, message);
        int n = 0;

        assertEquals(Type.Mood, parsed.get(n).type);
        assertEquals(Mood.Harsh, parsed.get(n++).value);
        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("Actor.jpg", parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);

        assertEquals(Type.Mood, parsed.get(n).type);
        assertEquals(Mood.Friendly, parsed.get(n++).value);
        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("foobar.jpg", parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);

        assertEquals(Type.Mood, parsed.get(n).type);
        assertEquals(Mood.Strict, parsed.get(n++).value);
        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("Actor.jpg", parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);

        assertEquals(Type.Mood, parsed.get(n).type);
        assertEquals(Mood.Friendly, parsed.get(n++).value);
        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("foo.jpg", parsed.get(n++).value);
        assertEquals(Type.Delay, parsed.get(n++).type);

        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("bar.jpg", parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);

        assertEquals(Type.Text, parsed.get(n++).type);

        assertEquals(Type.Mood, parsed.get(n).type);
        assertEquals(Mood.Happy, parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);

        assertEquals(parsed.size(), n);
    }

    @Test
    public void testThatNoImageTagApplyOverMultipleTextParagraphs() throws IOException {
        DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withOutput());
        script.actor.images = new RandomImages(Arrays.asList("actor.jpg"));
        script.setMood(Mood.Friendly);

        Message message = new Message(script.actor);
        message.add("There I am.");

        message.add(Message.NoImage);
        message.add("Nothing to see.");

        message.add("Still nothing to see.");

        RenderedMessage parsed = decorate(script, message);
        int n = 0;

        assertEquals(Type.Mood, parsed.get(n).type);
        assertEquals(Mood.Friendly, parsed.get(n++).value);

        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("actor.jpg", parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);

        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals(Message.NoImage, parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);

        assertEquals(Type.Text, parsed.get(n++).type);

        assertEquals(parsed.size(), n);
    }

    @Test
    public void testThatActorImageTagApplyOverMultipleTextParagraphs() throws IOException {
        DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withOutput());
        script.actor.images = new ActorTestImage("Actor.jpg");
        script.setMood(Mood.Friendly);

        Message message = new Message(script.actor);
        message.add(Message.NoImage);
        message.add("Nothing to see.");

        message.add(Message.ActorImage);
        message.add("There I am.");

        message.add("There I am again.");

        RenderedMessage parsed = decorate(script, message);
        int n = 0;

        assertEquals(Type.Mood, parsed.get(n).type);
        assertEquals(Mood.Friendly, parsed.get(n++).value);

        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals(Message.NoImage, parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);

        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("Actor.jpg", parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);

        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("Actor.jpg", parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);

        assertEquals(parsed.size(), n);
    }

    @Test
    public void testInjectionOfScriptImage() throws IOException {
        DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withOutput());

        Message message = new Message(script.actor);
        message.add("Some text.");
        script.setImage("foo.jpg");

        RenderedMessage parsed = decorate(script, message);
        AbstractMessage parts = parsed;
        int n = 0;
        assertEquals(Type.Mood, parts.get(n++).type);
        assertEquals(Type.Image, parts.get(n).type);
        assertEquals("foo.jpg", parts.get(n++).value);
        assertEquals(Type.Text, parts.get(n++).type);

        assertEquals(parts.size(), n);
    }

    @Test
    public void testInjectionOfScriptNoImage() throws IOException {
        DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withOutput());

        Message message = new Message(script.actor);
        message.add("Some text.");
        script.setImage(Message.NoImage);

        RenderedMessage parsed = decorate(script, message);
        int n = 0;
        assertEquals(Type.Mood, parsed.get(n++).type);
        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals(Message.NoImage, parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);

        assertEquals(parsed.size(), n);
    }

    @Test
    public void testInjectionOfImage() throws IOException {
        DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withInput().withOutput());

        Message message = new Message(script.actor);
        message.add("Some text.");
        script.setImage("foo.jpg");

        RenderedMessage parsed = decorate(script, message);
        int n = 0;
        assertEquals(Type.Mood, parsed.get(n++).type);
        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("foo.jpg", parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);

        assertEquals(parsed.size(), n);
    }

    @Test
    public void testInjectionOfNoImageInDebugSetup() throws IOException {
        DecoratingTestScript script = new DecoratingTestScript(new DebugSetup());

        Message message = new Message(script.actor);
        message.add("Some text.");
        script.setImage("foo.jpg");

        RenderedMessage parsed = decorate(script, message);
        int n = 0;
        assertEquals(Type.Mood, parsed.get(n++).type);
        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals(Message.NoImage, parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);

        assertEquals(parsed.size(), n);
    }

    @Test
    public void testMessageRendersWithMidDelay() throws IOException {
        DecoratingTestScript script = new DecoratingTestScript(new DebugSetup());
        script.debugger.freezeTime();

        Message message = new Message(script.actor);
        message.add("Some text.");
        message.add(Message.Delay120s);
        message.add("Some text.");

        RenderedMessage parsed = decorate(script, message);
        int n = 0;

        assertEquals(Type.Mood, parsed.get(n++).type);
        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals(Message.NoImage, parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);

        assertEquals(Type.Delay, parsed.get(n).type);
        assertEquals("120", parsed.get(n++).value);

        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals(Message.NoImage, parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);

        assertEquals(parsed.size(), n);

        assertMessageDuration(script, message, 120);
    }

    @Test
    public void testMessageRendersWithDelayAtEnd() throws IOException {
        DecoratingTestScript script = new DecoratingTestScript(new DebugSetup());
        script.debugger.freezeTime();

        Message message = new Message(script.actor);
        message.add("Some text.");
        message.add(Message.Delay120s);

        RenderedMessage parsed = decorate(script, message);
        int n = 0;

        assertEquals(Type.Mood, parsed.get(n++).type);
        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals(Message.NoImage, parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);
        assertEquals(Type.Delay, parsed.get(n).type);
        assertEquals("120", parsed.get(n++).value);

        assertEquals(parsed.size(), n);

        assertMessageDuration(script, message, 120);
    }

    @Test
    public void testMessageWithSpeech() throws IOException {
        DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withInput().withOutput());
        script.debugger.freezeTime();
        script.actor.images = new ActorTestImage("Actor.jpg");

        Message message = new Message(script.actor);
        message.add("Some text.");
        message.add(Type.Speech, "Some text.");
        message.add("Some more text.");
        message.add(Type.Speech, "Some more text.");

        RenderedMessage parsed = decorate(script, message);
        int n = 0;

        assertEquals(Type.Mood, parsed.get(n++).type);
        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("Actor.jpg", parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);
        assertEquals(Type.Speech, parsed.get(n++).type);
        assertEquals(ScriptMessageDecorator.DelayBetweenParagraphs, parsed.get(n++));

        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("Actor.jpg", parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);
        assertEquals(Type.Speech, parsed.get(n++).type);

        // assertEquals(Type.Keyword, parsed.get(n).type);
        // assertEquals(Message.ShowChoices, parsed.get(n++).value);
        //
        // assertEquals(ScriptMessageDecorator.DelayAtEndOfPage, parsed.get(n++));

        assertEquals(parsed.size(), n);
    }

    @Test
    public void testMessageWithSpeechAndDelayAtEnd() throws IOException {
        DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withInput().withOutput());
        script.debugger.freezeTime();
        script.actor.images = new ActorTestImage("Actor.jpg");

        Message message = new Message(script.actor);
        message.add("Some text.");
        message.add(Type.Speech, "Some text.");
        message.add("Some more text.");
        message.add(Type.Speech, "Some more text.");
        message.add(Type.Delay, "20");
        message.add("Even more text.");
        message.add(Type.Speech, "Even more text.");
        message.add(Type.Delay, "20");

        RenderedMessage parsed = decorate(script, message);
        int n = 0;

        assertEquals(Type.Mood, parsed.get(n++).type);
        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("Actor.jpg", parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);
        assertEquals(Type.Speech, parsed.get(n++).type);
        assertEquals(ScriptMessageDecorator.DelayBetweenParagraphs, parsed.get(n++));

        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("Actor.jpg", parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);
        assertEquals(Type.Speech, parsed.get(n++).type);

        assertEquals(Type.Delay, parsed.get(n).type);
        assertEquals("20", parsed.get(n++).value);

        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("Actor.jpg", parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);
        assertEquals(Type.Speech, parsed.get(n++).type);

        assertEquals(Type.Delay, parsed.get(n).type);
        assertEquals("20", parsed.get(n++).value);

        assertEquals(parsed.size(), n);
    }

    @Test
    public void testMessageWithExplicitShowChoicesKeyword() throws IOException {
        DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withInput().withOutput());
        script.debugger.freezeTime();
        script.actor.images = new ActorTestImage("Actor.jpg");

        Message message = new Message(script.actor);
        message.add("Some text.");
        message.add(Type.Speech, "Some text.");
        message.add(Type.Keyword, Message.ShowChoices);

        message.add("Some more text.");
        message.add(Type.Speech, "Some more text.");
        message.add(Type.Delay, "20");

        RenderedMessage parsed = decorate(script, message);
        int n = 0;

        assertEquals(Type.Mood, parsed.get(n++).type);
        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("Actor.jpg", parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);
        assertEquals(Type.Speech, parsed.get(n++).type);

        assertEquals(Type.Keyword, parsed.get(n).type);
        assertEquals(Message.ShowChoices, parsed.get(n++).value);

        assertEquals(ScriptMessageDecorator.DelayBetweenParagraphs, parsed.get(n++));

        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("Actor.jpg", parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);
        assertEquals(Type.Speech, parsed.get(n++).type);

        assertEquals(Type.Delay, parsed.get(n).type);
        assertEquals("20", parsed.get(n++).value);

        assertEquals(parsed.size(), n);
    }

    @Test
    public void testMessageWithImageAtEnd() throws IOException {
        DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withInput().withOutput());
        script.debugger.freezeTime();
        script.actor.images = new ActorTestImage("Actor.jpg");

        Message message = new Message(script.actor);
        message.add("Some text.");
        message.add(Type.Speech, "Some text.");
        message.add("Some more text.");
        message.add(Type.Speech, "Some more text.");
        message.add("foobar.jpg");

        RenderedMessage parsed = decorate(script, message);
        int n = 0;

        assertEquals(Type.Mood, parsed.get(n++).type);
        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("Actor.jpg", parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);
        assertEquals(Type.Speech, parsed.get(n++).type);
        assertEquals(ScriptMessageDecorator.DelayBetweenParagraphs, parsed.get(n++));

        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("Actor.jpg", parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);
        assertEquals(Type.Speech, parsed.get(n++).type);

        // assertEquals(Type.Keyword, parsed.get(n).type);
        // assertEquals(Message.ShowChoices, parsed.get(n++).value);
        // assertEquals(ScriptMessageDecorator.DelayAtEndOfPage, parsed.get(n++));

        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("foobar.jpg", parsed.get(n++).value);

        assertEquals(parsed.size(), n);
    }

    @Test
    public void testMessageWithSoundAtEnd() throws IOException {
        DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withInput().withOutput());
        script.debugger.freezeTime();
        script.actor.images = new ActorTestImage("Actor.jpg");

        Message message = new Message(script.actor);
        message.add("Some text.");
        message.add(Type.Speech, "Some text.");
        message.add("Some more text.");
        message.add(Type.Speech, "Some more text.");
        message.add("foobar.mp3");

        RenderedMessage parsed = decorate(script, message);
        int n = 0;

        assertEquals(Type.Mood, parsed.get(n++).type);
        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("Actor.jpg", parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);
        assertEquals(Type.Speech, parsed.get(n++).type);
        assertEquals(ScriptMessageDecorator.DelayBetweenParagraphs, parsed.get(n++));

        assertEquals(Type.Image, parsed.get(n).type);
        assertEquals("Actor.jpg", parsed.get(n++).value);
        assertEquals(Type.Text, parsed.get(n++).type);
        assertEquals(Type.Speech, parsed.get(n++).type);

        // assertEquals(Type.Keyword, parsed.get(n).type);
        // assertEquals(Message.ShowChoices, parsed.get(n++).value);
        // assertEquals(ScriptMessageDecorator.DelayAtEndOfPage, parsed.get(n++));

        assertEquals(Type.Sound, parsed.get(n).type);
        assertEquals("foobar.mp3", parsed.get(n++).value);

        assertEquals(parsed.size(), n);
    }

    @Test
    public void testMessageResourceList() {
        assertEquals(Arrays.asList("Foo.jpg", "Bar.mp3"), new Message(DummyActor, "Test.", "Foo.jpg",
                Message.ActorImage, "Test.", "Bar.mp3", Message.NoImage, "Test.").resources());
    }

    private static void assertMessageDuration(DecoratingTestScript script, Message message, long minimumSeconds) {
        long start = script.teaseLib.getTime(TimeUnit.SECONDS);
        script.renderMessage(message);
        script.completeMandatory();
        long end = script.teaseLib.getTime(TimeUnit.SECONDS);

        long duration = end - start;
        assertTrue(duration == minimumSeconds);
    }
}
