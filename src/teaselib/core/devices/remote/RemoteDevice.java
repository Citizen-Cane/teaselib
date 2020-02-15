package teaselib.core.devices.remote;

import java.util.Collections;

import teaselib.core.configuration.Configuration;
import teaselib.core.devices.BatteryLevel;
import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;

public abstract class RemoteDevice implements Device.Creatable {

    public static final RemoteDevice WaitingForConnection = new RemoteDevice() {
        @Override
        public String getDevicePath() {
            return Device.WaitingForConnection;
        }

        @Override
        public String getName() {
            return Device.WaitingForConnection;
        }

        @Override
        public boolean connected() {
            return false;
        }

        @Override
        public boolean active() {
            return false;
        }

        @Override
        public void close() {
        }

        @Override
        public boolean isWireless() {
            return true;
        }

        @Override
        public BatteryLevel batteryLevel() {
            return BatteryLevel.Medium;
        }

        @Override
        public String getServiceName() {
            return Device.WaitingForConnection;
        }

        @Override
        public String getDescription() {
            return Device.WaitingForConnection;
        }

        @Override
        public String getVersion() {
            return "0.0";
        }

        @Override
        public int sleep(int durationMinutes) {
            return 0;
        }

        @Override
        public RemoteDeviceMessage sendAndReceive(RemoteDeviceMessage message) {
            return Timeout;
        }

        @Override
        public void send(RemoteDeviceMessage message) {
        }

        @Override
        public String toString() {
            return getDevicePath();
        }
    };

    public static synchronized DeviceCache<RemoteDevice> getDeviceCache(Devices devices, Configuration configuration) {
        return new DeviceCache<RemoteDevice>().addFactory(LocalNetworkDevice.getDeviceFactory(devices, configuration))
                .addFactory(BluetoothDevice.getDeviceFactory(devices, configuration));
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
     * Enter sleep mode to save battery charge. During sleep mode the device may turn off network communications to save
     * power. Therefore the device might not react to any commands sent to it.
     * 
     * @param durationMinutes
     *            The requested sleep duration. The device may choose to sleep less time in order to complete services.
     *            If possible the device may enter deep sleep but this is not guaranteed.
     * @return The actual duration the device is going to sleep.
     */
    public abstract int sleep(int durationMinutes);
}
