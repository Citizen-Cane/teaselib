package teaselib.core.devices.remote;

import java.util.ArrayList;
import java.util.List;

import teaselib.core.devices.BatteryLevel;
import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;

public class RemoteDevices {

    public static final RemoteDevice WaitingForConnection = new RemoteDevice() {
        @Override
        public String getDevicePath() {
            return DeviceCache.createDevicePath(WaitingForConnection, WaitingForConnection);
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
        public boolean isWireless() {
            return true;
        }

        @Override
        public BatteryLevel batteryLevel() {
            return BatteryLevel.Medium;
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

    };

    public static List<RemoteDevice> devicesThatSupport(String serviceName, Devices devices) {
        List<RemoteDevice> remoteDevices = new ArrayList<>();
        DeviceCache<RemoteDevice> allRemoteDevices = devices.get(RemoteDevice.class);
        for (String devicePath : allRemoteDevices.getDevicePaths()) {
            if (!Device.WaitingForConnection.equals(DeviceCache.getDeviceName(devicePath))) {
                RemoteDevice device = allRemoteDevices.getDevice(devicePath);
                if (serviceName.equals(device.getServiceName())) {
                    remoteDevices.add(device);
                }
            }
        }
        return remoteDevices;
    }
}
