package teaselib.core.media;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Actor;
import teaselib.Message;
import teaselib.Message.Type;
import teaselib.MessagePart;
import teaselib.Mood;
import teaselib.Sexuality.Gender;
import teaselib.core.AbstractMessage;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.configuration.Setup;
import teaselib.test.ActorTestImage;
import teaselib.test.ActorTestImages;
import teaselib.test.TestScript;

public class MessagePartInjectionTest {

    private static final class DecoratingTestScript extends TestScript {

        public DecoratingTestScript(Setup setup) throws IOException {
            super(setup);
            actor.images = new ActorTestImage("Actor.jpg");
            debugger.freezeTime();
        }

        public RenderedMessage.Decorator[] getDecorators() {
            return new ScriptMessageDecorator(teaseLib.config, displayImage, actor, mood, resources,
                    this::expandTextVariables, Optional.empty()).all();
        }

        @Override
        public String expandTextVariables(String text) {
            return text;
        }

        public void renderMessage(Message message) {
            super.startMessage(message, false);
        }

        RenderedMessage decorate(Message message) {
            return RenderedMessage.of(message, getDecorators());
        }
    }

    @Test
    public void testEmptyMessage() throws IOException {
        try (DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withOutput())) {
            script.actor.instructions = new ActorTestImages("foobar.jpg");
            Message message = new Message(script.actor);
            script.setImage("foobar.jpg");
            RenderedMessage parsed = script.decorate(message);
            int n = 0;
            assertEquals(1, parsed.size());
            assertEquals(new MessagePart(Type.Image, "foobar.jpg"), parsed.get(n++));
        }
    }

    @Test
    public void testInjectionOfInlineResources() throws IOException {
        try (DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withOutput())) {
            script.actor.images = new ActorTestImage("Actor.jpg");
            script.actor.instructions = new ActorTestImages("foobar.jpg", "foo.jpg", "bar.jpg");

            Message message = new Message(script.actor);
            message.add("Some text.");

            message.add("foobar.jpg");
            message.add("Some more text.");

            message.add(Message.ActorImage);
            message.add("Even more text.");
            message.add(Type.Delay, "2");
            message.add("foo.jpg");
            message.add(Type.Delay, "2");

            message.add("bar.jpg");

            RenderedMessage parsed = script.decorate(message);
            int n = 0;

            assertEquals(Type.Mood, parsed.get(n++).type);
            assertEquals(Type.Image, parsed.get(n).type);
            assertEquals("Actor.jpg", parsed.get(n++).value);

            assertEquals(new MessagePart(Type.Text, "Some text."), parsed.get(n++));
            assertEquals(ScriptMessageDecorator.DelayBetweenParagraphs, parsed.get(n++));

            assertEquals(new MessagePart(Type.Image, "foobar.jpg"), parsed.get(n++));
            assertEquals(Type.Text, parsed.get(n++).type);
            assertEquals(ScriptMessageDecorator.DelayBetweenParagraphs, parsed.get(n++));

            assertEquals(new MessagePart(Type.Image, "Actor.jpg"), parsed.get(n++));
            assertEquals(Type.Text, parsed.get(n++).type);
            assertEquals(Type.Delay, parsed.get(n++).type);

            assertEquals(new MessagePart(Type.Image, "foo.jpg"), parsed.get(n++));
            assertEquals(Type.Delay, parsed.get(n++).type);

            assertEquals(new MessagePart(Type.Image, "bar.jpg"), parsed.get(n++));

            assertEquals(parsed.size(), n);
        }
    }

    @Test
    public void testInjectionOfMood() throws IOException {
        try (DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withOutput())) {
            script.actor.images = new ActorTestImage("Actor.jpg");
            script.actor.instructions = new ActorTestImages("foobar.jpg", "foo.jpg", "bar.jpg");

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

            RenderedMessage parsed = script.decorate(message);
            int n = 0;

            assertEquals(new MessagePart(Type.Mood, Mood.Harsh), parsed.get(n++));
            assertEquals(new MessagePart(Type.Image, "Actor.jpg"), parsed.get(n++));
            assertEquals(Type.Text, parsed.get(n++).type);
            assertEquals(ScriptMessageDecorator.DelayBetweenParagraphs, parsed.get(n++));

            assertEquals(new MessagePart(Type.Mood, Mood.Friendly), parsed.get(n++));
            assertEquals(new MessagePart(Type.Image, "foobar.jpg"), parsed.get(n++));
            assertEquals(Type.Text, parsed.get(n++).type);
            assertEquals(ScriptMessageDecorator.DelayBetweenParagraphs, parsed.get(n++));

            assertEquals(new MessagePart(Type.Mood, Mood.Strict), parsed.get(n++));
            assertEquals(new MessagePart(Type.Image, "Actor.jpg"), parsed.get(n++));
            assertEquals(Type.Text, parsed.get(n++).type);
            assertEquals(ScriptMessageDecorator.DelayBetweenParagraphs, parsed.get(n++));

            assertEquals(new MessagePart(Type.Mood, Mood.Friendly), parsed.get(n++));
            assertEquals(new MessagePart(Type.Image, "foo.jpg"), parsed.get(n++));
            assertEquals(Type.Delay, parsed.get(n++).type);

            assertEquals(new MessagePart(Type.Image, "bar.jpg"), parsed.get(n++));
            assertEquals(Type.Text, parsed.get(n++).type);
            assertEquals(ScriptMessageDecorator.DelayBetweenParagraphs, parsed.get(n++));

            assertEquals(Type.Text, parsed.get(n++).type);
            assertEquals(ScriptMessageDecorator.DelayBetweenParagraphs, parsed.get(n++));

            assertEquals(new MessagePart(Type.Mood, Mood.Happy), parsed.get(n++));
            assertEquals(Type.Text, parsed.get(n++).type);

            assertEquals(parsed.size(), n);
        }
    }

    @Test
    public void testThatNoImageTagApplyOverMultipleTextParagraphs() throws IOException {
        try (DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withOutput())) {
            script.actor.images = new ActorTestImage("actor.jpg");
            script.setMood(Mood.Friendly);

            Message message = new Message(script.actor);
            message.add("There I am.");

            message.add(Message.NoImage);
            message.add("Nothing to see.");

            message.add("Still nothing to see.");

            RenderedMessage parsed = script.decorate(message);
            int n = 0;

            assertEquals(Type.Mood, parsed.get(n).type);
            assertEquals(Mood.Friendly, parsed.get(n++).value);

            assertEquals(Type.Image, parsed.get(n).type);
            assertEquals("actor.jpg", parsed.get(n++).value);
            assertEquals(Type.Text, parsed.get(n++).type);
            assertEquals(ScriptMessageDecorator.DelayBetweenParagraphs, parsed.get(n++));

            assertEquals(Type.Image, parsed.get(n).type);
            assertEquals(Message.NoImage, parsed.get(n++).value);
            assertEquals(Type.Text, parsed.get(n++).type);
            assertEquals(ScriptMessageDecorator.DelayBetweenParagraphs, parsed.get(n++));

            assertEquals(Type.Text, parsed.get(n++).type);

            assertEquals(parsed.size(), n);
        }
    }

    @Test
    public void testThatActorImageTagApplyOverMultipleTextParagraphs() throws IOException {
        try (DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withOutput())) {
            script.actor.images = new ActorTestImage("Actor.jpg");
            script.setMood(Mood.Friendly);

            Message message = new Message(script.actor);
            message.add(Message.NoImage);
            message.add("Nothing to see.");

            message.add(Message.ActorImage);
            message.add("There I am.");

            message.add("There I am again.");

            RenderedMessage parsed = script.decorate(message);
            int n = 0;

            assertEquals(Type.Mood, parsed.get(n).type);
            assertEquals(Mood.Friendly, parsed.get(n++).value);

            assertEquals(Type.Image, parsed.get(n).type);
            assertEquals(Message.NoImage, parsed.get(n++).value);
            assertEquals(Type.Text, parsed.get(n++).type);
            assertEquals(ScriptMessageDecorator.DelayBetweenParagraphs, parsed.get(n++));

            assertEquals(Type.Image, parsed.get(n).type);
            assertEquals("Actor.jpg", parsed.get(n++).value);
            assertEquals(Type.Text, parsed.get(n++).type);
            assertEquals(ScriptMessageDecorator.DelayBetweenParagraphs, parsed.get(n++));

            assertEquals(Type.Image, parsed.get(n).type);
            assertEquals("Actor.jpg", parsed.get(n++).value);
            assertEquals(Type.Text, parsed.get(n++).type);

            assertEquals(parsed.size(), n);
        }

    }

    @Test
    public void testInjectionOfScriptImage() throws IOException {
        try (DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withOutput())) {
            script.actor.instructions = new ActorTestImage("foo.jpg");
            Message message = new Message(script.actor);
            message.add("Some text.");
            script.setImage("foo.jpg");

            RenderedMessage parsed = script.decorate(message);
            AbstractMessage parts = parsed;
            int n = 0;
            assertEquals(Type.Mood, parts.get(n++).type);
            assertEquals(Type.Image, parts.get(n).type);
            assertEquals("foo.jpg", parts.get(n++).value);
            assertEquals(Type.Text, parts.get(n++).type);

            assertEquals(parts.size(), n);
        }
    }

    @Test
    public void testThatAppendingToSentenceDoesntResultInPauseBetweenParts() throws IOException {
        try (DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withOutput())) {
            Message message = new Message(script.actor);
            message.add("Some text,");
            message.add("and some more.");

            RenderedMessage parsed = script.decorate(message);
            AbstractMessage parts = parsed;
            int n = 0;
            assertEquals(Type.Mood, parts.get(n++).type);
            assertEquals(Type.Image, parts.get(n++).type);
            assertEquals(Type.Text, parts.get(n++).type);
            assertEquals(ScriptMessageDecorator.DelayAfterAppend, parsed.get(n++));

            assertEquals(Type.Image, parts.get(n++).type);
            assertEquals(Type.Text, parts.get(n++).type);

            assertEquals(parts.size(), n);
        }
    }

    @Test
    public void testInjectionOfScriptNoImage() throws IOException {
        try (DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withOutput())) {
            Message message = new Message(script.actor);
            message.add("Some text.");
            script.setImage(Message.NoImage);

            RenderedMessage parsed = script.decorate(message);
            int n = 0;
            assertEquals(Type.Mood, parsed.get(n++).type);
            assertEquals(Type.Image, parsed.get(n).type);
            assertEquals(Message.NoImage, parsed.get(n++).value);
            assertEquals(Type.Text, parsed.get(n++).type);

            assertEquals(parsed.size(), n);
        }
    }

    @Test
    public void testInjectionOfImage() throws IOException {
        try (DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withOutput())) {
            script.actor.instructions = new ActorTestImages("foo.jpg");
            Message message = new Message(script.actor);
            message.add("Some text.");
            script.setImage("foo.jpg");

            RenderedMessage parsed = script.decorate(message);
            int n = 0;
            assertEquals(Type.Mood, parsed.get(n++).type);
            assertEquals(Type.Image, parsed.get(n).type);
            assertEquals("foo.jpg", parsed.get(n++).value);
            assertEquals(Type.Text, parsed.get(n++).type);

            assertEquals(parsed.size(), n);
        }
    }

    @Test
    public void testInjectionOfNoImageInDebugSetupWithoutOutput() throws IOException {
        try (DecoratingTestScript script = new DecoratingTestScript(new DebugSetup())) {
            Message message = new Message(script.actor);
            message.add("Some text.");
            script.setImage("foo.jpg");

            RenderedMessage parsed = script.decorate(message);
            int n = 0;
            assertEquals(Type.Mood, parsed.get(n++).type);
            assertEquals(Type.Image, parsed.get(n).type);
            assertEquals(Message.NoImage, parsed.get(n++).value);
            assertEquals(Type.Text, parsed.get(n++).type);

            assertEquals(parsed.size(), n);
        }
    }

    @Test
    public void testMessageRendersWithMidDelay() throws IOException {
        try (DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withOutput())) {
            Message message = new Message(script.actor);
            message.add("Some text.");
            message.add(Message.Delay120s);
            message.add("Some text.");

            RenderedMessage parsed = script.decorate(message);
            int n = 0;

            assertEquals(Type.Mood, parsed.get(n++).type);
            assertEquals(Type.Image, parsed.get(n).type);
            assertEquals("Actor.jpg", parsed.get(n++).value);
            assertEquals(Type.Text, parsed.get(n++).type);

            assertEquals(Type.Delay, parsed.get(n).type);
            assertEquals("120", parsed.get(n++).value);

            assertEquals(Type.Image, parsed.get(n).type);
            assertEquals("Actor.jpg", parsed.get(n++).value);
            assertEquals(Type.Text, parsed.get(n++).type);

            assertEquals(parsed.size(), n);

            assertMessageDuration(script, message, 120);
        }
    }

    @Test
    public void testMessageRendersWithDelayAtEnd() throws IOException {
        try (DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withOutput())) {
            Message message = new Message(script.actor);
            message.add("Some text.");
            message.add(Message.Delay120s);

            RenderedMessage parsed = script.decorate(message);
            int n = 0;

            assertEquals(Type.Mood, parsed.get(n++).type);
            assertEquals(Type.Image, parsed.get(n).type);
            assertEquals("Actor.jpg", parsed.get(n++).value);
            assertEquals(Type.Text, parsed.get(n++).type);
            assertEquals(Type.Delay, parsed.get(n).type);
            assertEquals("120", parsed.get(n++).value);

            assertEquals(parsed.size(), n);

            assertMessageDuration(script, message, 120);
        }
    }

    @Test
    public void testMessageWithSpeech() throws IOException {
        try (DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withOutput())) {
            Message message = new Message(script.actor);
            message.add("Some text.");
            message.add(Type.Speech, "Some text.");
            message.add("Some more text.");
            message.add(Type.Speech, "Some more text.");

            RenderedMessage parsed = script.decorate(message);
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

            assertEquals(parsed.size(), n);
        }
    }

    @Test
    public void testMessageWithSpeechDoesntDelayAfterAppend() throws IOException {
        try (DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withOutput())) {
            Message message = new Message(script.actor);
            message.add("Some text,");
            message.add(Type.Speech, "Some text,");
            message.add("plus some more appended after the comma of the first part.");
            message.add(Type.Speech, "plus some more appended after the comma of the first part.");

            RenderedMessage parsed = script.decorate(message);
            int n = 0;

            assertEquals(Type.Mood, parsed.get(n++).type);
            assertEquals(Type.Image, parsed.get(n).type);
            assertEquals("Actor.jpg", parsed.get(n++).value);
            assertEquals(Type.Text, parsed.get(n++).type);
            assertEquals(Type.Speech, parsed.get(n++).type);
            assertEquals(ScriptMessageDecorator.DelayAfterAppend, parsed.get(n++));

            assertEquals(Type.Image, parsed.get(n).type);
            assertEquals("Actor.jpg", parsed.get(n++).value);
            assertEquals(Type.Text, parsed.get(n++).type);
            assertEquals(Type.Speech, parsed.get(n++).type);

            assertEquals(parsed.size(), n);
        }
    }

    @Test
    public void testMessageWithSpeechAndDelayAtEnd() throws IOException {
        try (DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withOutput())) {
            Message message = new Message(script.actor);
            message.add("Some text.");
            message.add(Type.Speech, "Some text.");
            message.add("Some more text.");
            message.add(Type.Speech, "Some more text.");
            message.add(Type.Delay, "20");
            message.add("Even more text.");
            message.add(Type.Speech, "Even more text.");
            message.add(Type.Delay, "20");

            RenderedMessage parsed = script.decorate(message);
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
    }

    @Test
    public void testMessageWithExplicitShowChoicesKeyword() throws IOException {
        try (DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withOutput())) {
            Message message = new Message(script.actor);
            message.add("Some text.");
            message.add(Type.Speech, "Some text.");
            message.add(Type.Keyword, Message.ShowChoices);

            message.add("Some more text.");
            message.add(Type.Speech, "Some more text.");
            message.add(Type.Delay, "20");

            RenderedMessage parsed = script.decorate(message);
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
    }

    @Test
    public void testMessageWithImageAtEnd() throws IOException {
        try (DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withOutput())) {
            script.actor.instructions = new ActorTestImage("foobar.jpg");
            Message message = new Message(script.actor);
            message.add("Some text.");
            message.add(Type.Speech, "Some text.");
            message.add("Some more text.");
            message.add(Type.Speech, "Some more text.");
            message.add("foobar.jpg");

            RenderedMessage parsed = script.decorate(message);
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

            assertEquals(Type.Image, parsed.get(n).type);
            assertEquals("foobar.jpg", parsed.get(n++).value);

            assertEquals(parsed.size(), n);
        }
    }

    @Test
    public void testMessageWithSoundAtEnd() throws IOException {
        try (DecoratingTestScript script = new DecoratingTestScript(new DebugSetup().withOutput())) {
            Message message = new Message(script.actor);
            message.add("Some text.");
            message.add(Type.Speech, "Some text.");
            message.add("Some more text.");
            message.add(Type.Speech, "Some more text.");
            message.add("foobar.mp3");

            RenderedMessage parsed = script.decorate(message);
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
            assertEquals(Type.Sound, parsed.get(n).type);
            assertEquals("foobar.mp3", parsed.get(n++).value);

            assertEquals(parsed.size(), n);
        }
    }

    @Test
    public void testMessageResourceList() {
        assertEquals(Arrays.asList("Foo.jpg", "Bar.mp3"), new Message(new Actor("Foo", Gender.Masculine, Locale.UK),
                "Test.", "Foo.jpg", Message.ActorImage, "Test.", "Bar.mp3", Message.NoImage, "Test.").resources());
    }

    private static void assertMessageDuration(DecoratingTestScript script, Message message, long minimumSeconds) {
        script.debugger.advanceTimeAllThreads();

        long start = script.teaseLib.getTime(TimeUnit.SECONDS);
        script.renderMessage(message);
        script.awaitMandatoryCompleted();
        long end = script.teaseLib.getTime(TimeUnit.SECONDS);

        long duration = end - start;
        assertEquals(minimumSeconds, duration);
    }

}
