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
        assertEquals(testMessage.command, restored.command);
        assertEquals(testMessage.parameters, restored.parameters);
    }

    @Test
    public void testByteArrayPersistAndRestoreBinaryPart() throws IOException {
        UDPMessage testMessage = new UDPMessage("test",
                Arrays.asList("parameter 1", "parameter 2", "parameter 3"),
                new byte[] { 1, 2, 3 });
        byte[] data = testMessage.toByteArray();
        UDPMessage restored = new UDPMessage(data);
        assertEquals(testMessage.command, restored.command);
        assertEquals(testMessage.parameters, restored.parameters);
        assertEquals(testMessage.binary.length, restored.binary.length);
        for (int i = 0; i < testMessage.binary.length; i++) {
            assertEquals(testMessage.binary[i], restored.binary[i]);
        }
    }

    @Test
    public void testPersistAndRestoreNoParametersBinaryPart()
            throws IOException {
        UDPMessage testMessage = new UDPMessage("test", new ArrayList<String>(),
                new byte[] { 1, 2, 3 });
        byte[] data = testMessage.toByteArray();
        UDPMessage restored = new UDPMessage(data);
        assertEquals(testMessage.command, restored.command);
        assertEquals(0, restored.parameters.size());
        assertEquals(testMessage.binary.length, restored.binary.length);
        for (int i = 0; i < testMessage.binary.length; i++) {
            assertEquals(testMessage.binary[i], restored.binary[i]);
        }
    }

    @Test
    public void testPersistAndRestoreEmptyCommand() throws IOException {
        UDPMessage testMessage = new UDPMessage("test");
        byte[] data = testMessage.toByteArray();
        UDPMessage restored = new UDPMessage(data);
        assertEquals(testMessage.command, restored.command);
        assertEquals(0, restored.parameters.size());
        assertEquals(0, restored.binary.length);
        for (int i = 0; i < testMessage.binary.length; i++) {
            assertEquals(testMessage.binary[i], restored.binary[i]);
        }
    }

    @Test
    public void testByteArrayContentUnderstood() throws IOException {
        UDPMessage testMessage = new UDPMessage("command",
                Arrays.asList("parameter1", "param2"), new byte[] { 1, 2, 3 });
        // test restore
        byte[] data = testMessage.toByteArray();
        UDPMessage restored = new UDPMessage(data);
        assertEquals(testMessage.command, restored.command);
        assertEquals(2, restored.parameters.size());
        assertEquals(testMessage.binary.length, restored.binary.length);
        for (int i = 0; i < testMessage.binary.length; i++) {
            assertEquals(testMessage.binary[i], restored.binary[i]);
        }
        // test byte array content
        int textSize = 256 * data[0] + data[1];
        // text
        assertEquals(testMessage.parameters.size(), data[2]);
        assertEquals(1 + testMessage.command.length() + 1
                + testMessage.parameters.get(0).length() + 1
                + testMessage.parameters.get(1).length() + 1, textSize);
        // string ends
        assertEquals(0, data[(2 + 1 + testMessage.command.length() + 1) - 1]);
        assertEquals(0, data[(2 + 1 + testMessage.command.length() + 1
                + testMessage.parameters.get(0).length() + 1) - 1]);
        assertEquals(0,
                data[(2 + 1 + testMessage.command.length() + 1
                        + testMessage.parameters.get(0).length() + 1
                        + testMessage.parameters.get(1).length() + 1) - 1]);
        assertEquals(testMessage.parameters.size(), data[2]);
        // binary size
        int binaryStartIndex = 2 + textSize;
        assertEquals(256 * data[binaryStartIndex] + data[binaryStartIndex + 1],
                testMessage.binary.length);
        // binary content
        for (int i = 0; i < testMessage.binary.length; i++) {
            assertEquals(data[binaryStartIndex + 2 + i], testMessage.binary[i]);
        }
        // Sanity - sum of sizes
        assertEquals(data.length, 2 + textSize + 2 + testMessage.binary.length);
    }
}
