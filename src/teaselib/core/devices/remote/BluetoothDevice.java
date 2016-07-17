/**
 * 
 */
package teaselib.core.devices.remote;

import java.util.ArrayList;
import java.util.List;

import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.DeviceCache.Factory;

/**
 * @author someone
 *
 */
public class BluetoothDevice implements RemoteDevice {
    private static final String DeviceClassName = "BluetoothDevice";

    public static final DeviceCache.Factory<RemoteDevice> Factory = new DeviceCache.Factory<RemoteDevice>() {
        @Override
        public String getDeviceClass() {
            return BluetoothDevice.DeviceClassName;
        }

        @Override
        public List<String> getDevices() {
            List<String> deviceNames = new ArrayList<String>();
            // TODO scan
            // TODO available devices
            // TODO available services on the device
            // TODO create a device for each service
            return deviceNames;
        }

        @Override
        public BluetoothDevice getDevice(String path) {
            return new BluetoothDevice();
        }
    };

    @Override
    public String getDevicePath() {
        throw new IllegalStateException(
                "Bluetooth remote devices are not implemnted yet");
    }

    @Override
    public String getName() {
        throw new IllegalStateException(
                "Bluetooth remote devices are not implemnted yet");
    }

    @Override
    public boolean active() {
        throw new IllegalStateException(
                "Bluetooth remote devices are not implemnted yet");
    }

    @Override
    public void release() {
        throw new IllegalStateException(
                "Bluetooth remote devices are not implemnted yet");
    }

    @Override
    public String getServiceName() {
        throw new IllegalStateException(
                "Bluetooth remote devices are not implemnted yet");
    }

    @Override
    public String getVersion() {
        throw new IllegalStateException(
                "Bluetooth remote devices are not implemnted yet");
    }

}
