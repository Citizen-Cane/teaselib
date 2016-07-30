/**
 * 
 */
package teaselib.core.devices.remote;

import java.util.Collections;
import java.util.List;

/**
 * @author someone
 *
 */
public class RemoteDeviceMessage {
    public final String command;
    public final List<String> parameters;
    public final byte[] binary;

    private static String join(String a, String b) {
        return a + " " + b;
    }

    public RemoteDeviceMessage(String service, String command) {
        this(join(service, command), Collections.EMPTY_LIST, new byte[0]);
    }

    public RemoteDeviceMessage(String command) {
        this(command, Collections.EMPTY_LIST, new byte[0]);
    }

    public RemoteDeviceMessage(String service, String command,
            List<String> parameters) {
        this(join(service, command), parameters, new byte[0]);
    }

    public RemoteDeviceMessage(String command, List<String> parameters) {
        this(command, parameters, new byte[0]);
    }

    public RemoteDeviceMessage(String service, String command,
            List<String> parameters, byte[] binary) {
        this(join(service, command), parameters, binary);
    }

    public RemoteDeviceMessage(String command, List<String> parameters,
            byte[] binary) {
        super();
        this.command = command;
        this.parameters = parameters;
        this.binary = binary;
    }
}
