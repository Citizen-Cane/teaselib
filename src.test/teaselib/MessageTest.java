/**
 * 
 */
package teaselib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static teaselib.Message.ActorImage;
import static teaselib.Message.Delay10s;
import static teaselib.Mood.Amused;
import static teaselib.Mood.Angry;

import java.util.List;
import java.util.Locale;

import org.junit.Test;

import teaselib.core.texttospeech.Voice;

/**
 * @author someone
 *
 */
public class MessageTest {

    private static final Actor actor = new Actor("Test", Voice.Female, Locale.US);

    @Test
    public void determineType() {
        assertTrue(Message.determineType("Understood, #slave?") == Message.Type.Text);
    }

    @Test
    public void unexpectedSpacesAtEnd() {
        // Trailing white space before is okay
        final String s = "Understood, #slave? ";
        assertEquals(Message.Type.Text, Message.determineType(s));
        final Message message = new Message(actor, s);
        assertEquals(message.size(), 1);
        assertTrue(message.get(0).value.endsWith("?"));
    }

    @Test
    public void unexpectedSpacesBefore() {
        // Leading white space before is okay
        final String s = " Understood, #slave?";
        assertEquals(Message.Type.Text, Message.determineType(s));
        final Message message = new Message(actor, s);
        assertEquals(message.size(), 1);
        assertTrue(message.get(0).value.startsWith("U"));
    }

    @Test
    public void missingEndOfSentence() {
        assertEquals(Message.Type.Text, Message.determineType("Have you Understood, #slave"));
    }

    @Test
    public void properAdd() {
        // Leading and trailing white space before is okay
        Message message = new Message(actor, " Have you Understood, #slave ");
        assertEquals(Message.Type.Text, message.get(0).type);
    }

    @Test
    public void properParagraphs() {
        // Leading and trailing white space before is okay
        Message message = new Message(actor, "Now go!", "Have you Understood, #slave?");
        assertEquals(2, message.size());
        assertEquals(Message.Type.Text, message.get(0).type);
        assertEquals(Message.Type.Text, message.get(1).type);
    }

    @Test
    public void dontConcatenate() {
        // Leading and trailing white space before is okay
        Message message = new Message(actor, "Now go and", "do it, #slave!");
        assertEquals(2, message.size());
        assertEquals(Message.Type.Text, message.get(0).type);
    }

    @Test
    public void properConcatenation() {
        // Leading and trailing white space before is okay
        Message message = new Message(actor, "   Now go and   ", "  do it, #slave!   ").joinSentences();
        assertEquals(1, message.size());
        assertEquals(Message.Type.Text, message.get(0).type);
        assertEquals("Now go and do it, #slave!", message.get(0).value);
    }

    @Test
    public void readAloudSimple() {
        // Leading and trailing white space before is okay
        Message message = new Message(actor, "\"The adventures of Terry and Sally\"").readAloud();
        assertEquals(3, message.size());
        assertEquals(Message.Type.Mood, message.get(0).type);
        assertEquals(Mood.Reading, message.get(0).value);
        assertEquals(Message.Type.Text, message.get(1).type);
        assertEquals(Message.Type.Mood, message.get(2).type);
        assertEquals(Mood.Neutral, message.get(2).value);
    }

    @Test
    public void readAloudInbetween() {
        // Leading and trailing white space before is okay
        Message message = new Message(actor, "It's storytime, slut!",
                "I'm going to relate to you a fun tale of self-bondage today;", "a chapter from one of My favorites:",
                "\"The adventures of Terry and Sally\"", "I'm sure you'll like it.").readAloud();
        assertEquals(7, message.size());
        assertEquals(Message.Type.Text, message.get(0).type);
        assertEquals(Message.Type.Text, message.get(1).type);
        assertEquals(Message.Type.Text, message.get(2).type);
        assertEquals(Message.Type.Mood, message.get(3).type);
        assertEquals(Mood.Reading, message.get(3).value);
        assertEquals(Message.Type.Text, message.get(4).type);
        assertEquals(Message.Type.Mood, message.get(5).type);
        assertEquals(Mood.Neutral, message.get(5).value);
        assertEquals(Message.Type.Text, message.get(6).type);
    }

    @Test
    public void testJoinSentencesBuilderFunction() {
        // Leading and trailing white space before is okay
        Message message = new Message(actor, "The idea that we came up with was simple.",
                "We would tie ourselves up out in the guest room using our locking wrist cuffs",
                "and put the keys in different locations throughout the yard, garage and house.",
                "We would therefore have to leave the guest room if we wanted to get free.",
                "The interesting part to this whole adventure was the techniques that",
                "Jennifer and I used to keep us both in the guest room for a few hours", "prior to getting out.")
                        .joinSentences();
        assertEquals(4, message.size());
        assertEquals(Message.Type.Text, message.get(0).type);
        assertEquals(Message.Type.Text, message.get(1).type);
        assertEquals(Message.Type.Text, message.get(2).type);
        assertEquals(Message.Type.Text, message.get(3).type);
    }

    @Test
    public void testReadAloudBuilderFunction() {
        // Leading and trailing white space before is okay
        Message message = new Message(actor, "It's storytime, slut!",
                "I'm going to relate to you a fun tale of self-bondage today;", "a chapter from one of My favorites:",
                "\"The adventures of Terry and Sally\"", "I'm sure you'll like it.").readAloud();
        assertEquals(7, message.size());
        assertEquals(Message.Type.Text, message.get(0).type);
        assertEquals(Message.Type.Text, message.get(1).type);
        assertEquals(Message.Type.Text, message.get(2).type);
        assertEquals(Message.Type.Mood, message.get(3).type);
        assertEquals(Mood.Reading, message.get(3).value);
        assertEquals(Message.Type.Text, message.get(4).type);
        assertEquals(Message.Type.Mood, message.get(5).type);
        assertEquals(Mood.Neutral, message.get(5).value);
        assertEquals(Message.Type.Text, message.get(6).type);
        assertTrue(!message.get(6).value.startsWith(Mood.Neutral));
    }

    @Test
    public void testSubordinateClauseConcatenationComma() {
        // concat == true
        Message message = new Message(actor, "Care must be taken to not make the ties too strong,",
                "in order to keep the blood flowing through the tied up limbs.").joinSentences();
        assertEquals(2, message.size());
        assertEquals(Message.Type.Text, message.get(0).type);
        assertEquals(Message.Type.Text, message.get(1).type);
    }

    @Test
    public void testSubordinateClauseConcatenationDash() {
        // concat == true
        Message message = new Message(actor, "I want you to put the gag in, to begin with -",
                "you'll be wearing it throughout the session.").joinSentences();
        assertEquals(2, message.size());
        assertEquals(Message.Type.Text, message.get(0).type);
        assertEquals(Message.Type.Text, message.get(1).type);
    }

    @Test
    public void testSplit() {
        Message message = new Message(actor, "Foo.", "Bar.");

        List<Message> messages = message.split();
        assertEquals(2, messages.size());

        Message joined = Message.join(messages);
        assertEquals(message, joined);
    }

    @Test
    public void testSplitMoodImage() {
        Message message = new Message(actor, ActorImage, Amused, "Foo.", ActorImage, Mood.Angry, "Bar.");

        List<Message> messages = message.split();
        assertEquals(2, messages.size());

        Message joined = Message.join(messages);
        assertEquals(message, joined);
    }

    @Test
    public void testSplitMoodMultipleImages() {
        testMultipleImages(new Message(actor, ActorImage, Amused, "Foo.", "Foobar.wav", ActorImage, Angry, "Bar."));
        testMultipleImages(new Message(actor, ActorImage, Amused, "Foo.", "Foobar.wav", ActorImage, Angry, "Bar."));
        testMultipleImages(new Message(actor, "Foobar.jpg", Amused, "Foo.", "Foobar.wav", ActorImage, Angry, "Bar."));
        testMultipleImages(new Message(actor, "Foo.jpg", Amused, "Foo.", "Foobar.wav", "Bar.jpg", Mood.Angry, "Bar."));

        testMultipleImages(new Message(actor, Amused, ActorImage, "Foo.", "Foobar.wav", ActorImage, Angry, "Bar."));
        testMultipleImages(new Message(actor, Amused, ActorImage, "Foo.", "Foobar.wav", Angry, ActorImage, "Bar."));
        testMultipleImages(new Message(actor, "Foobar.jpg", Amused, "Foo.", "Foobar.wav", Angry, ActorImage, "Bar."));
        testMultipleImages(new Message(actor, "Foo.jpg", Amused, "Foo.", "Foobar.wav", "Bar.jpg", Angry, "Bar."));

        testMultipleImages(new Message(actor, Amused, ActorImage, Delay10s, "Foo.wav", "Foo.", Delay10s, "Bar.wav"));
    }

    @Test
    public void testSplitTrickyMessages() {
        List<Message> messages = new Message(actor, "Foo", "Foobar.jpg", "Bar", "Foobar").split();
        assertEquals(3, messages.size());

        assertEquals(1, messages.get(0).size());
        assertEquals(2, messages.get(1).size());
        assertEquals(1, messages.get(2).size());
    }

    @Test
    public void testSplitNoText() {
        List<Message> messages = new Message(actor, Delay10s, "Foo.wav", Delay10s, "Foobar.jpg", "").split();
        assertEquals(2, messages.size());

        assertEquals(3, messages.get(0).size());
        assertEquals(2, messages.get(1).size());
    }

    private void testMultipleImages(Message message) {
        List<Message> messages = message.split();
        assertEquals(2, messages.size());

        assertEquals(4, messages.get(0).size());
        assertEquals(Message.Type.Sound, messages.get(0).get(3).type);

        assertEquals(3, messages.get(1).size());

        Message joined = Message.join(messages);
        assertEquals(message, joined);
    }
}
