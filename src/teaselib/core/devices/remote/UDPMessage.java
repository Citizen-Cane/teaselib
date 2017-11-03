package teaselib.core.devices.remote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * @author Citizen-Cane@github.com
 *         <p>
 *         Message format:
 *         <p>
 *         <ul>
 *         <li>short text part size : the size of the text part (without this size info)
 *         <li>byte parameter count : number of parameters after the name
 *         <li>String command: the command name, null-terminated
 *         <li>List<String> parameters: all parameters, null-terminated
 *         <li>short binary size: The size of the binary part of the message (without this size info)
 *         <li>byte[size] binary data: the binary data of the message
 *         </ul>
 *         <p>
 */
public class UDPMessage {
    private static final String Encoding = "UTF-8";

    public final RemoteDeviceMessage message;

    static boolean isValid(byte[] data, int startIndex) {
        int offset = 0;
        int textSize = 256 * data[startIndex + offset++] + data[startIndex + offset++];
        int parameterCount = data[startIndex + offset++];
        int commandNameLength = strlen(data, startIndex + offset);
        offset += commandNameLength + 1;
        for (int i = 0; i < parameterCount; i++) {
            int parameterLength = strlen(data, startIndex + offset);
            offset += parameterLength + 1;
        }
        if (offset - 2 != textSize) {
            return false;
        }
        if (data.length >= offset + 2) {
            int binarySize = 256 * data[startIndex + offset++] + data[startIndex + offset++];
            if (data.length < offset + binarySize) {
                return false;
            }
            offset += binarySize;
        }
        return data.length == offset;
    }

    /**
     * Return string length in byte array
     * 
     * @param data
     * @param i
     * @return The number of bytes to the next 0
     */
    private static int strlen(byte[] data, int start) {
        int n = 0;
        for (int i = start; i < data.length; i++) {
            if (data[i] == 0)
                break;
            n++;
        }
        return n;
    }

    public UDPMessage(RemoteDeviceMessage message) {
        this.message = message;
    }

    public UDPMessage(String command, String... parameters) {
        this(command, Arrays.asList(parameters), new byte[0]);

    }

    public UDPMessage(String command, List<String> parameters, byte[] binary) {
        this.message = new RemoteDeviceMessage(command, parameters, binary);
    }

    public UDPMessage(byte[] data) throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(data);
        DataInputStream dataInputStream = new DataInputStream(input);
        int textDataSize = dataInputStream.readShort();
        byte[] textData = new byte[textDataSize - 1];
        int parameterCount = dataInputStream.readByte();
        dataInputStream.readFully(textData);
        Scanner scanner = new Scanner(new ByteArrayInputStream(textData), Encoding);
        try {
            scanner.useDelimiter("\u0000");
            String name = scanner.next();
            List<String> parameters = new ArrayList<>(parameterCount);
            while (scanner.hasNext() && parameters.size() < parameterCount) {
                parameters.add(scanner.next());
            }
            int binarySize = dataInputStream.readShort();
            byte[] binary = new byte[binarySize];
            try {
                if (binarySize > 0) {
                    dataInputStream.readFully(binary);
                }
            } finally {
                dataInputStream.close();
            }
            message = new RemoteDeviceMessage(name, parameters, binary);
        } finally {
            scanner.close();
        }
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(data);
        byte[] textData = getTextData();
        output.writeShort(textData.length);
        output.write(textData);
        output.writeShort(message.binary.length);
        if (message.binary.length > 0) {
            output.write(message.binary);
        }
        data.close();
        return data.toByteArray();
    }

    private byte[] getTextData() throws IOException {
        ByteArrayOutputStream textData = new ByteArrayOutputStream();
        DataOutputStream textDataOuptut = new DataOutputStream(textData);
        textDataOuptut.writeByte(message.parameters.size());
        textDataOuptut.write(message.command.getBytes());
        textDataOuptut.writeByte(0);
        for (String parameter : message.parameters) {
            textDataOuptut.write(parameter.getBytes());
            textDataOuptut.write(0);
        }
        return textData.toByteArray();
    }
}
