package teaselib.core.devices.release;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import teaselib.core.Configuration;
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
                return new KeyRelease(device, devices, this);
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

    private static final String Actuators = "actuators";

    private RemoteDevice remoteDevice;
    private final Devices devices;
    private final DeviceFactory<KeyRelease> factory;

    private String[] releaseKeys = { "" };

    KeyRelease(Devices devices, DeviceFactory<KeyRelease> factory) {
        this(RemoteDevices.WaitingForConnection, devices, factory);
    }

    KeyRelease(RemoteDevice remoteDevice, Devices devices, DeviceFactory<KeyRelease> factory) {
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

    public List<Actuator> actuators() {
        RemoteDeviceMessage count = remoteDevice.sendAndReceive(new RemoteDeviceMessage(DeviceClassName, Actuators));
        if (RemoteDevice.Count.equals(count.command)) {
            int actuators = Integer.parseInt(count.parameters.get(0));
            releaseKeys = new String[actuators];
            List<Actuator> releaseMchanisms = new ArrayList<>(actuators);
            for (int i = 0; i < releaseKeys.length; i++) {
                releaseMchanisms.add(new Actuator(this, i));
                releaseKeys[i] = "";
            }
            return releaseMchanisms;
        } else {
            return Collections.emptyList();
        }
    }

    public Actuator getActuator(long duration, TimeUnit unit) {
        List<Actuator> releaseMechanisms = actuators();
        List<Long> durations = releaseMechanisms.stream().map(actuator -> actuator.available(unit))
                .collect(Collectors.toList());
        return releaseMechanisms.get(getActuatorIndex(duration, durations));
    }

    static int getActuatorIndex(long duration, List<Long> durations) {
        long bestDifferenceSoFar = Integer.MAX_VALUE;
        int unset = Integer.MIN_VALUE;
        int bestActuator = unset;
        long maxDuration = unset;
        int maxActuator = unset;
        for (int actuator = 0; actuator < durations.size(); actuator++) {
            long availableDuration = durations.get(actuator);
            long difference = availableDuration - duration;
            if (0 <= difference && difference < bestDifferenceSoFar) {
                bestActuator = actuator;
                bestDifferenceSoFar = difference;
            }
            if (availableDuration > maxDuration) {
                maxDuration = availableDuration;
                maxActuator = actuator;
            }
        }
        return bestActuator != unset ? bestActuator : maxActuator;
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
            return "";
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
