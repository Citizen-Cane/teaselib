package teaselib.core.devices.remote;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import teaselib.core.devices.DeviceFactory;

public class BluetoothDevice implements RemoteDevice {
    private static final String DeviceClassName = "BluetoothDevice";

    public static final DeviceFactory<RemoteDevice> Factory = new DeviceFactory<RemoteDevice>(
            DeviceClassName) {
        @Override
        public List<String> enumerateDevicePaths(
                Map<String, RemoteDevice> deviceCache) {
            List<String> deviceNames = new ArrayList<String>();
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
    };

    public BluetoothDevice(@SuppressWarnings("unused") String deviceName) {
        super();
    }

    @Override
    public String getDevicePath() {
        throw new IllegalStateException(
                "Bluetooth remote devices are not implemented yet");
    }

    @Override
    public String getName() {
        throw new IllegalStateException(
                "Bluetooth remote devices are not implemented yet");
    }

    @Override
    public boolean connected() {
        throw new IllegalStateException(
                "Bluetooth remote devices are not implemented yet");
    }

    @Override
    public boolean active() {
        throw new IllegalStateException(
                "Bluetooth remote devices are not implemented yet");
    }

    @Override
    public void close() {
        throw new IllegalStateException(
                "Bluetooth remote devices are not implemented yet");
    }

    @Override
    public String getServiceName() {
        throw new IllegalStateException(
                "Bluetooth remote devices are not implemented yet");
    }

    @Override
    public String getDescription() {
        throw new IllegalStateException(
                "Bluetooth remote devices are not implemented yet");
    }

    @Override
    public String getVersion() {
        throw new IllegalStateException(
                "Bluetooth remote devices are not implemented yet");
    }

    @Override
    public RemoteDeviceMessage sendAndReceive(RemoteDeviceMessage message) {
        throw new IllegalStateException(
                "Bluetooth remote devices are not implemented yet");
    }

    @Override
    public void send(RemoteDeviceMessage message) {
        throw new IllegalStateException(
                "Bluetooth remote devices are not implemented yet");
    }

}
