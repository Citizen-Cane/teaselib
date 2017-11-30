package teaselib.core.devices.remote;

import java.util.Collections;

import teaselib.core.Configuration;
import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;

public abstract class RemoteDevice implements Device.Creatable {

    private static DeviceCache<RemoteDevice> Instance;

    public static synchronized DeviceCache<RemoteDevice> getDeviceCache(Devices devices, Configuration configuration) {
        if (Instance == null) {
            Instance = new DeviceCache<RemoteDevice>()
                    .addFactory(LocalNetworkDevice.getDeviceFactory(devices, configuration))
                    .addFactory(BluetoothDevice.getDeviceFactory(devices, configuration));
        }
        return Instance;
    }

    static final RemoteDeviceMessage Id = new RemoteDeviceMessage("id", Collections.emptyList(), new byte[] {});
    static final RemoteDeviceMessage Timeout = new RemoteDeviceMessage("timeout", Collections.emptyList(),
            new byte[] {});
    static final RemoteDeviceMessage Error = new RemoteDeviceMessage("error", Collections.emptyList(), new byte[] {});

    /**
     * Set the device to sleep
     */
    public static final String Sleep = "sleep";

    /**
     * A numerical value, returned by the device
     */
    public static final String Count = "count";

    public abstract String getServiceName();

    public abstract String getDescription();

    public abstract String getVersion();

    public abstract RemoteDeviceMessage sendAndReceive(RemoteDeviceMessage message);

    public abstract void send(RemoteDeviceMessage message);

    /**
     * Sleep to save battery charge. During sleep mode the device may turn off network communications to save power.
     * Therefore the device might not react to any commands sent to it.
     * 
     * @param durationMinutes
     *            The requested sleep duration. The device may choose to sleep less time in order to complete services.
     *            If possible the device may enter deep sleep but this is not guaranteed.
     * @return The actual duration the device is going to sleep.
     */
    public abstract int sleep(int durationMinutes);
}
