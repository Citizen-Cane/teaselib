package teaselib.core.devices.remote;

import java.util.Collections;

import teaselib.core.devices.Device;

public interface RemoteDevice extends Device {

    static final RemoteDeviceMessage Id = new RemoteDeviceMessage("id",
            Collections.EMPTY_LIST, new byte[] {});

    static final RemoteDeviceMessage Timeout = new RemoteDeviceMessage(
            "timeout", Collections.EMPTY_LIST, new byte[] {});

    static final RemoteDeviceMessage Error = new RemoteDeviceMessage("error",
            Collections.EMPTY_LIST, new byte[] {});

    /**
     * Set the device to sleep
     */
    public static final String Sleep = "sleep";

    /**
     * A numerical value, returned by the device
     */
    public static final String Count = "count";

    String getServiceName();

    String getDescription();

    String getVersion();

    RemoteDeviceMessage sendAndReceive(RemoteDeviceMessage message);

    void send(RemoteDeviceMessage message);

    /**
     * Sleep to save battery charge. During sleep mode the device may turn off
     * network communications to save power. Therefore the device might not
     * react to any commands sent to it.
     * 
     * @param durationMinutes
     *            The requested sleep duration. The device may choose to sleep
     *            less time in order to complete services. If possible the
     *            device may enter deep sleep but this is not guaranteed.
     * @return The actual duration the device is going to sleep.
     */
    int sleep(int durationMinutes);
}
