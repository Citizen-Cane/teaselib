/**
 * 
 */
package teaselib;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author someone
 *
 */
public class MessageTest {

    private static final Actor actor = new Actor(Actor.Dominant, "en-us");

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
        assertEquals(message.getParts().size(), 1);
        assertTrue(message.getParts().get(0).value.endsWith("?"));
    }

    @Test
    public void unexpectedSpacesBefore() {
        // Leading white space before is okay
        final String s = " Understood, #slave?";
        assertEquals(Message.Type.Text, Message.determineType(s));
        final Message message = new Message(actor, s);
        assertEquals(message.getParts().size(), 1);
        assertTrue(message.getParts().get(0).value.startsWith("U"));
    }

    @Test
    public void missingEndOfSentence() {
        assertEquals(Message.Type.Text,
                Message.determineType("Have you Understood, #slave"));
    }

    @Test
    public void properAdd() {
        // Leading and trailing white space before is okay
        Message message = new Message(actor, " Have you Understood, #slave ");
        assertEquals(Message.Type.Text, message.getParts().get(0).type);
    }

    @Test
    public void properParagraphs() {
        // Leading and trailing white space before is okay
        Message message = new Message(actor, "Now go!",
                "Have you Understood, #slave?");
        assertEquals(message.getParts().size(), 2);
        assertEquals(Message.Type.Text, message.getParts().get(0).type);
        assertEquals(Message.Type.Text, message.getParts().get(1).type);
    }

    @Test
    public void properConcatenation() {
        // Leading and trailing white space before is okay
        Message message = new Message(actor, "Now go and", "do it, #slave!");
        assertEquals(message.getParts().size(), 1);
        assertEquals(Message.Type.Text, message.getParts().get(0).type);
    }

    @Test
    public void readAloud() {
        // Leading and trailing white space before is okay
        Message message = new Message(actor,
                "\"The adventures of Terry and Sally\"");
        assertEquals(message.getParts().size(), 3);
        assertEquals(Message.Type.Mood, message.getParts().get(0).type);
        assertEquals(Mood.Reading, message.getParts().get(0).value);
        assertEquals(Message.Type.Text, message.getParts().get(1).type);
        assertEquals(Message.Type.Mood, message.getParts().get(2).type);
        assertEquals(Mood.Neutral, message.getParts().get(2).value);
    }
}
