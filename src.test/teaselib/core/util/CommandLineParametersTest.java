package teaselib.core.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class CommandLineParametersTest {
    enum Keywords {
        Apply,
        Remove,
        Items
    }

    @Test
    public void testEqualsIgnoresCase() throws Exception {
        CommandLineParameters<Keywords> cmd = new CommandLineParameters<>(
                Arrays.asList("Apply", "A", "b", "c", "Remove", "d", "E", "f", "Items", "d", "e", "F"),
                Keywords.values(), Keywords.class);

        assertEquals(cmd,
                new CommandLineParameters<>(
                        Arrays.asList("Apply", "A", "b", "c", "Remove", "d", "E", "f", "Items", "d", "e", "F"),
                        Keywords.values(), Keywords.class));

        assertEquals(cmd,
                new CommandLineParameters<>(
                        Arrays.asList("apply", "a", "B", "c", "Remove", "d", "E", "F", "Items", "D", "e", "f"),
                        Keywords.values(), Keywords.class));

        assertNotEquals(cmd,
                new CommandLineParameters<>(
                        Arrays.asList("FOO", "A", "b", "c", "Remove", "d", "E", "f", "Items", "d", "e", "F"),
                        Keywords.values(), Keywords.class));

        assertNotEquals(cmd,
                new CommandLineParameters<>(
                        Arrays.asList("A", "bar", "c", "Remove", "d", "E", "f", "Items", "d", "e", "F"),
                        Keywords.values(), Keywords.class));
    }

}
