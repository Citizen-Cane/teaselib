package teaselib.core.devices.remote;

import java.util.List;
import java.util.Vector;

import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceCache;

public class RemoteDevices {

    public static final DeviceCache<RemoteDevice> Instance = new DeviceCache<RemoteDevice>()
            .addFactory(LocalNetworkDevice.Factory)
            .addFactory(BluetoothDevice.Factory);

    static final RemoteDevice WaitingForConnection = new RemoteDevice() {

        @Override
        public String getDevicePath() {
            return DeviceCache.createDevicePath(WaitingForConnection,
                    WaitingForConnection);
        }

        @Override
        public String getName() {
            return WaitingForConnection;
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
        public String getServiceName() {
            return WaitingForConnection;
        }

        @Override
        public String getDescription() {
            return WaitingForConnection;
        }

        @Override
        public String getVersion() {
            return "0.0";
        }

        @Override
        public RemoteDeviceMessage sendAndReceive(RemoteDeviceMessage message) {
            return Timeout;
        }

        @Override
        public void send(RemoteDeviceMessage message) {
        }

    };

    public static List<RemoteDevice> devicesThatSupport(String serviceName) {
        List<RemoteDevice> remoteDevices = new Vector<RemoteDevice>();
        for (String devicePath : Instance.getDevicePaths()) {
            if (!Device.WaitingForConnection
                    .equals(DeviceCache.getDeviceName(devicePath))) {
                RemoteDevice device = Instance.getDevice(devicePath);
                if (serviceName.equals(device.getServiceName())) {
                    remoteDevices.add(device);
                }
            }
        }
        return remoteDevices;
    }
}
