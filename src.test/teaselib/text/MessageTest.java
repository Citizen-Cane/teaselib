/**
 * 
 */
package teaselib.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import teaselib.Actor;
import teaselib.Message;

/**
 * @author someone
 *
 */
public class MessageTest {

    @Test
    public void determineType() {
        assertTrue(Message.determineType("Understood, #slave?") == Message.Type.Text);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unexpectedSpacesAtEnd() {
        // Trailing white space before is bad
        Message.determineType("Understood, #slave? ");
    }

    @Test
    public void unexpectedSpacesBefore() {
        // Leading white space before is okay
        Message.determineType(" Understood, #slave?");
    }

    @Test
    public void missingEndOfSentence() {
        // Leading white space before is okay
        Message.determineType("Have you Understood, #slave");
    }

    @Test
    public void properAdd() {
        // Leading white space before is okay
        Message message = new Message(new Actor(Actor.Dominant, "de-de"),
                "Have you Understood, #slave ");
        assertEquals(Message.Type.Text, message.getParts().get(0).type);
    }
}
