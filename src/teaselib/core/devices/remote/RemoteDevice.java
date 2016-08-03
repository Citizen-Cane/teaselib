package teaselib.core.devices.remote;

import java.util.Collections;

import teaselib.core.devices.Device;

public interface RemoteDevice extends Device {

    static final RemoteDeviceMessage Id = new RemoteDeviceMessage("id",
            Collections.EMPTY_LIST, new byte[] {});

    static final RemoteDeviceMessage Timeout = new RemoteDeviceMessage(
            "timeout", Collections.EMPTY_LIST, new byte[] {});

    String getServiceName();

    String getDescription();

    String getVersion();

    RemoteDeviceMessage sendAndReceive(RemoteDeviceMessage message);

    void send(RemoteDeviceMessage message);
}
