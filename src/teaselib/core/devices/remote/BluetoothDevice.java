package teaselib.core.devices.remote;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import teaselib.core.configuration.Configuration;
import teaselib.core.devices.BatteryLevel;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.DeviceFactory;
import teaselib.core.devices.Devices;

public class BluetoothDevice extends RemoteDevice {
    private static final class BluetoothDeviceFactory extends DeviceFactory<RemoteDevice> {
        BluetoothDeviceFactory(String deviceClass, Devices devices, Configuration configuration) {
            super(deviceClass, devices, configuration);
        }

        @Override
        public List<String> enumerateDevicePaths(Map<String, RemoteDevice> deviceCache) {
            List<String> deviceNames = new ArrayList<>();
            // TODO scan
            // TODO available devices
            // TODO available services on the device
            // TODO create a device for each service
            return deviceNames;
        }

        @Override
        public RemoteDevice createDevice(String deviceName) {
            return new BluetoothDevice(deviceName);
        }
    }

    private static final String DeviceClassName = "BluetoothDevice";

    public static synchronized BluetoothDeviceFactory getDeviceFactory(Devices devices, Configuration configuration) {
        return new BluetoothDeviceFactory(DeviceClassName, devices, configuration);
    }

    public static synchronized DeviceCache<RemoteDevice> getDeviceCache(Devices devices, Configuration configuration) {
        return devices.get(RemoteDevice.class);
    }

    public BluetoothDevice(@SuppressWarnings("unused") String deviceName) {
        super();
    }

    @Override
    public String getDevicePath() {
        throw new IllegalStateException("Bluetooth remote devices are not implemented yet");
    }

    @Override
    public String getName() {
        throw new IllegalStateException("Bluetooth remote devices are not implemented yet");
    }

    @Override
    public boolean connected() {
        throw new IllegalStateException("Bluetooth remote devices are not implemented yet");
    }

    @Override
    public boolean active() {
        throw new IllegalStateException("Bluetooth remote devices are not implemented yet");
    }

    @Override
    public void close() {
        throw new IllegalStateException("Bluetooth remote devices are not implemented yet");
    }

    @Override
    public String getServiceName() {
        throw new IllegalStateException("Bluetooth remote devices are not implemented yet");
    }

    @Override
    public String getDescription() {
        throw new IllegalStateException("Bluetooth remote devices are not implemented yet");
    }

    @Override
    public String getVersion() {
        throw new IllegalStateException("Bluetooth remote devices are not implemented yet");
    }

    @Override
    public int sleep(int durationMinutes) {
        return 0;
    }

    @Override
    public RemoteDeviceMessage sendAndReceive(RemoteDeviceMessage message) {
        throw new IllegalStateException("Bluetooth remote devices are not implemented yet");
    }

    @Override
    public void send(RemoteDeviceMessage message) {
        throw new IllegalStateException("Bluetooth remote devices are not implemented yet");
    }

    @Override
    public boolean isWireless() {
        throw new IllegalStateException("Bluetooth remote devices are not implemented yet");
    }

    @Override
    public BatteryLevel batteryLevel() {
        throw new IllegalStateException("Bluetooth remote devices are not implemented yet");
    }

}
