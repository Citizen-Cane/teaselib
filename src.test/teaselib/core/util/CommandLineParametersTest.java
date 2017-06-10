package teaselib.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;

import org.junit.Test;

public class CommandLineParametersTest {
    enum Keywords {
        Apply,
        Remove,
        Items
    }

    @Test
    public void testEqualsIgnoresCase() throws Exception {
        CommandLineParameters<Keywords> cmd = new CommandLineParameters<Keywords>(
                Arrays.asList("Apply", "A", "b", "c", "Remove", "d", "E", "f", "Items", "d", "e", "F"),
                Keywords.values());

        assertEquals(cmd,
                new CommandLineParameters<Keywords>(
                        Arrays.asList("Apply", "A", "b", "c", "Remove", "d", "E", "f", "Items", "d", "e", "F"),
                        Keywords.values()));

        assertEquals(cmd,
                new CommandLineParameters<Keywords>(
                        Arrays.asList("apply", "a", "B", "c", "Remove", "d", "E", "F", "Items", "D", "e", "f"),
                        Keywords.values()));

        assertNotEquals(cmd,
                new CommandLineParameters<Keywords>(
                        Arrays.asList("FOO", "A", "b", "c", "Remove", "d", "E", "f", "Items", "d", "e", "F"),
                        Keywords.values()));

        assertNotEquals(cmd, new CommandLineParameters<Keywords>(
                Arrays.asList("A", "bar", "c", "Remove", "d", "E", "f", "Items", "d", "e", "F"), Keywords.values()));
    }

}
