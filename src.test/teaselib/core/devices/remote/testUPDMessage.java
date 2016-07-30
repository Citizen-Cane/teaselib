package teaselib.core.devices.remote;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

public class testUPDMessage {

    @Test
    public void testByteArrayPeristsAndRestore() throws IOException {
        UDPMessage testMessage = new UDPMessage("test", "parameter 1",
                "parameter 2", "parameter 3");
        byte[] data = testMessage.toByteArray();
        UDPMessage restored = new UDPMessage(data);
        assertEquals(testMessage.message.command, restored.message.command);
        assertEquals(testMessage.message.parameters,
                restored.message.parameters);
    }

    @Test
    public void testByteArrayPersistAndRestoreBinaryPart() throws IOException {
        UDPMessage testMessage = new UDPMessage("test",
                Arrays.asList("parameter 1", "parameter 2", "parameter 3"),
                new byte[] { 1, 2, 3 });
        byte[] data = testMessage.toByteArray();
        UDPMessage restored = new UDPMessage(data);
        assertEquals(testMessage.message.command, restored.message.command);
        assertEquals(testMessage.message.parameters,
                restored.message.parameters);
        assertEquals(testMessage.message.binary.length,
                restored.message.binary.length);
        for (int i = 0; i < testMessage.message.binary.length; i++) {
            assertEquals(testMessage.message.binary[i],
                    restored.message.binary[i]);
        }
    }

    @Test
    public void testPersistAndRestoreNoParametersBinaryPart()
            throws IOException {
        UDPMessage testMessage = new UDPMessage("test", new ArrayList<String>(),
                new byte[] { 1, 2, 3 });
        byte[] data = testMessage.toByteArray();
        UDPMessage restored = new UDPMessage(data);
        assertEquals(testMessage.message.command, restored.message.command);
        assertEquals(0, restored.message.parameters.size());
        assertEquals(testMessage.message.binary.length,
                restored.message.binary.length);
        for (int i = 0; i < testMessage.message.binary.length; i++) {
            assertEquals(testMessage.message.binary[i],
                    restored.message.binary[i]);
        }
    }

    @Test
    public void testPersistAndRestoreEmptyCommand() throws IOException {
        UDPMessage testMessage = new UDPMessage("test");
        byte[] data = testMessage.toByteArray();
        UDPMessage restored = new UDPMessage(data);
        assertEquals(testMessage.message.command, restored.message.command);
        assertEquals(0, restored.message.parameters.size());
        assertEquals(0, restored.message.binary.length);
        for (int i = 0; i < testMessage.message.binary.length; i++) {
            assertEquals(testMessage.message.binary[i],
                    restored.message.binary[i]);
        }
    }

    @Test
    public void testByteArrayContentUnderstood() throws IOException {
        UDPMessage testMessage = new UDPMessage("command",
                Arrays.asList("parameter1", "param2"), new byte[] { 1, 2, 3 });
        // test restore
        byte[] data = testMessage.toByteArray();
        UDPMessage restored = new UDPMessage(data);
        assertEquals(testMessage.message.command, restored.message.command);
        assertEquals(2, restored.message.parameters.size());
        assertEquals(testMessage.message.binary.length,
                restored.message.binary.length);
        for (int i = 0; i < testMessage.message.binary.length; i++) {
            assertEquals(testMessage.message.binary[i],
                    restored.message.binary[i]);
        }
        // test byte array content
        int textSize = 256 * data[0] + data[1];
        // text
        assertEquals(testMessage.message.parameters.size(), data[2]);
        assertEquals(
                1 + testMessage.message.command.length() + 1
                        + testMessage.message.parameters.get(0).length() + 1
                        + testMessage.message.parameters.get(1).length() + 1,
                textSize);
        // string ends
        assertEquals(0,
                data[(2 + 1 + testMessage.message.command.length() + 1) - 1]);
        assertEquals(0,
                data[(2 + 1 + testMessage.message.command.length() + 1
                        + testMessage.message.parameters.get(0).length() + 1)
                        - 1]);
        assertEquals(0,
                data[(2 + 1 + testMessage.message.command.length() + 1
                        + testMessage.message.parameters.get(0).length() + 1
                        + testMessage.message.parameters.get(1).length() + 1)
                        - 1]);
        assertEquals(testMessage.message.parameters.size(), data[2]);
        // binary size
        int binaryStartIndex = 2 + textSize;
        assertEquals(256 * data[binaryStartIndex] + data[binaryStartIndex + 1],
                testMessage.message.binary.length);
        // binary content
        for (int i = 0; i < testMessage.message.binary.length; i++) {
            assertEquals(data[binaryStartIndex + 2 + i],
                    testMessage.message.binary[i]);
        }
        // Sanity - sum of sizes
        assertEquals(data.length,
                2 + textSize + 2 + testMessage.message.binary.length);
        // The final test - UDPMessage.isValid() method is correct
        assertTrue(UDPMessage.isValid(data, 0));
    }
}
