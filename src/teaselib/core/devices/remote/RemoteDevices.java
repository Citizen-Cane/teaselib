package teaselib.core.devices.remote;

import java.util.List;
import java.util.Vector;

import teaselib.core.ScriptInterruptedException;
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

    /**
     * Poll for connection until the device is connected or the timeout duration
     * is exceeded.
     * <p>
     * The poll frequency depends on the the runtime duration of each connect
     * call, but will be at least 1 second.
     * 
     * @param device
     *            The device to connect
     * @param timeoutSeconds
     *            The maximum time to poll.
     * @return True if the device is connected.
     * @throws ScriptInterruptedException
     */
    public static boolean connect(Device device, double timeoutSeconds)
            throws ScriptInterruptedException {
        long timeoutMillis = (long) timeoutSeconds * 1000;
        while (timeoutMillis > 0) {
            long now = System.currentTimeMillis();
            if (device.connected()) {
                return true;
            }
            long elapsed = System.currentTimeMillis() - now;
            if (elapsed > timeoutMillis) {
                return false;
            }
            long duration = Math.min(timeoutMillis,
                    Math.max(1000, elapsed * 10));
            try {
                Thread.sleep(duration);
                timeoutMillis -= duration;
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            }
        }
        return device.connected();
    }
}
