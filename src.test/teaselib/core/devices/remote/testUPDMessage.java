package teaselib.core.devices.remote;

import static org.junit.Assert.assertEquals;

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
}
