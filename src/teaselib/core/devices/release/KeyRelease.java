package teaselib.core.devices.release;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import teaselib.core.configuration.Configuration;
import teaselib.core.devices.BatteryLevel;
import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.DeviceFactory;
import teaselib.core.devices.Devices;
import teaselib.core.devices.remote.RemoteDevice;
import teaselib.core.devices.remote.RemoteDeviceMessage;
import teaselib.core.devices.remote.RemoteDevices;

public class KeyRelease implements Device, Device.Creatable {
    private static final class MyDeviceFactory extends DeviceFactory<KeyRelease> {
        private MyDeviceFactory(String deviceClass, Devices devices, Configuration configuration) {
            super(deviceClass, devices, configuration);
        }

        @Override
        public List<String> enumerateDevicePaths(Map<String, KeyRelease> deviceCache) {
            List<String> devicePaths = new ArrayList<>();
            for (RemoteDevice remoteDevice : RemoteDevices.devicesThatSupport(DeviceClassName, devices)) {
                devicePaths.add(DeviceCache.createDevicePath(DeviceClassName, remoteDevice.getDevicePath()));
            }
            return devicePaths;
        }

        @Override
        public KeyRelease createDevice(String deviceName) {
            if (WaitingForConnection.equals(deviceName)) {
                return new KeyRelease(devices, this);
            } else {
                DeviceCache<RemoteDevice> deviceCache = devices.get(RemoteDevice.class);
                RemoteDevice device = deviceCache.getDevice(deviceName);
                return new KeyRelease(devices, this, device);
            }
        }
    }

    public static synchronized MyDeviceFactory getDeviceFactory(Devices devices, Configuration configuration) {
        return new MyDeviceFactory(DeviceClassName, devices, configuration);
    }

    public static synchronized DeviceCache<KeyRelease> getDeviceCache(Devices devices, Configuration configuration) {
        return new DeviceCache<KeyRelease>().addFactory(getDeviceFactory(devices, configuration));
    }

    /**
     * Returned by the device to indicate successful command
     */
    private static final String Ok = "ok";

    /**
     * Arm the device
     */
    private static final String Arm = "arm";

    /**
     * Hold the device actuator until the timer starts
     */
    private static final String Hold = "hold";

    /**
     * Start the timer
     */
    private static final String Start = "start";

    /**
     * add minutes until release up to the hard limit
     */
    private static final String Add = "add";

    /**
     * Whether the actuator is running, e.g. time elapses
     */
    private static final String Running = "running";

    /**
     * Ask for available minutes
     */
    private static final String Available = "available";

    /**
     * Ask for remaining minutes
     */
    private static final String Remaining = "remaining";

    /**
     * Release a key
     */
    private static final String Release = "release";

    /**
     * a session key, returned by the device.
     */
    private static final String ReleaseKey = "releasekey";

    private static final String DeviceClassName = "KeyRelease";

    private static final String GetActuators = "actuators";

    private RemoteDevice remoteDevice;
    private final Devices devices;
    private final DeviceFactory<KeyRelease> factory;

    private Actuators actuators;
    private String[] releaseKeys = { "" };

    KeyRelease(Devices devices, DeviceFactory<KeyRelease> factory) {
        this(devices, factory, RemoteDevices.WaitingForConnection);
    }

    KeyRelease(Devices devices, DeviceFactory<KeyRelease> factory, RemoteDevice remoteDevice) {
        this.remoteDevice = remoteDevice;
        this.devices = devices;
        this.factory = factory;
    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(DeviceClassName, remoteDevice.getDevicePath());
    }

    @Override
    public String getName() {
        return remoteDevice.getName() + " " + remoteDevice.getServiceName() + " " + remoteDevice.getDescription() + " "
                + remoteDevice.getVersion();
    }

    @Override
    public boolean connected() {
        if (remoteDevice == RemoteDevices.WaitingForConnection) {
            List<RemoteDevice> keyReleaseDevices = RemoteDevices.devicesThatSupport(DeviceClassName, devices);
            if (!keyReleaseDevices.isEmpty()) {
                remoteDevice = keyReleaseDevices.get(0);
                factory.connectDevice(this);
            }
            return remoteDevice.connected();
        } else {
            return remoteDevice.connected();
        }
    }

    @Override
    public boolean active() {
        return remoteDevice.active();
    }

    @Override
    public void close() {
        remoteDevice.close();
    }

    @Override
    public boolean isWireless() {
        return remoteDevice.isWireless();
    }

    @Override
    public BatteryLevel batteryLevel() {
        return remoteDevice.batteryLevel();
    }

    public Actuators actuators() {
        if (actuators != null) {
            return actuators;
        } else if (connected()) {
            RemoteDeviceMessage count = remoteDevice
                    .sendAndReceive(new RemoteDeviceMessage(DeviceClassName, GetActuators));
            if (RemoteDevice.Count.equals(count.command)) {
                int size = Integer.parseInt(count.parameters.get(0));
                releaseKeys = new String[size];
                List<Actuator> elements = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    elements.add(new KeyReleaseActuator(this, i));
                    releaseKeys[i] = "";
                }
                actuators = new Actuators(elements);
                return actuators;
            } else {
                return Actuators.NONE;
            }
        } else {
            return Actuators.NONE;
        }
    }

    boolean arm(int actuator) {
        RemoteDeviceMessage ok = remoteDevice.sendAndReceive(
                new RemoteDeviceMessage(DeviceClassName, Arm, Arrays.asList(Integer.toString(actuator))));
        releaseKeys[actuator] = "";
        return Ok.equals(ok.command);
    }

    String start(int actuator, int seconds) {
        RemoteDeviceMessage key = remoteDevice.sendAndReceive(new RemoteDeviceMessage(DeviceClassName, Start,
                Arrays.asList(Integer.toString(actuator), Integer.toString(seconds))));
        if (ReleaseKey.equals(key.command)) {
            releaseKeys[actuator] = key.parameters.get(0);
            return releaseKeys[actuator];
        } else {
            throw new IllegalStateException(key.command);
        }
    }

    String hold(int actuator) {
        RemoteDeviceMessage key = remoteDevice.sendAndReceive(
                new RemoteDeviceMessage(DeviceClassName, Hold, Arrays.asList(Integer.toString(actuator))));
        if (ReleaseKey.equals(key.command)) {
            releaseKeys[actuator] = key.parameters.get(0);
            return releaseKeys[actuator];
        } else {
            throw new IllegalStateException(key.command);
        }
    }

    int sleep(int seconds) {
        return remoteDevice.sleep(seconds);
    }

    boolean add(int actuator, int seconds) {
        RemoteDeviceMessage ok = remoteDevice.sendAndReceive(new RemoteDeviceMessage(DeviceClassName, Add,
                Arrays.asList(Integer.toString(actuator), Integer.toString(seconds))));
        return Ok.equals(ok.command);
    }

    /**
     * Whether any of the actuators is holding a key.
     * 
     * @param actuator
     *            The actuator.
     * @return True if the actuator holds a key and is counting down.
     */
    boolean isRunning(int actuator) {
        RemoteDeviceMessage count = remoteDevice.sendAndReceive(
                new RemoteDeviceMessage(DeviceClassName, Running, Arrays.asList(Integer.toString(actuator))));
        if (RemoteDevice.Count.equals(count.command)) {
            return Integer.parseInt(count.parameters.get(0)) == 1;
        } else {
            return false;
        }
    }

    /**
     * Get the number of minutes that can be added to this actuator.
     * 
     * @param actuator
     * @return The number of available minutes.
     */
    int available(int actuator) {
        RemoteDeviceMessage count = remoteDevice.sendAndReceive(
                new RemoteDeviceMessage(DeviceClassName, Available, Arrays.asList(Integer.toString(actuator))));
        if (RemoteDevice.Count.equals(count.command)) {
            return Integer.parseInt(count.parameters.get(0));
        } else {
            return 0;
        }
    }

    /**
     * @param actuator
     * @return Duration minutes until release.
     */
    int remaining(int actuator) {
        RemoteDeviceMessage count = remoteDevice.sendAndReceive(
                new RemoteDeviceMessage(DeviceClassName, Remaining, Arrays.asList(Integer.toString(actuator))));
        if (RemoteDevice.Count.equals(count.command)) {
            return Integer.parseInt(count.parameters.get(0));
        } else {
            return 0;
        }
    }

    /**
     * Release a key.
     * 
     * @param actuator
     *            THe actuator that holds the key.
     * @return Whether the key has been released.
     */
    boolean release(int actuator) {
        RemoteDeviceMessage released = remoteDevice.sendAndReceive(new RemoteDeviceMessage(DeviceClassName, Release,
                Arrays.asList(Integer.toString(actuator), releaseKeys[actuator])));
        return Ok.equals(released.command);
    }

    @Override
    public String toString() {
        return remoteDevice.getDescription();
    }
}
