package teaselib.core.devices.remote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import teaselib.core.devices.BatteryLevel;
import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.DeviceFactory;

public class KeyRelease implements Device {

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

    private static final DeviceFactory<KeyRelease> Factory = new DeviceFactory<KeyRelease>(
            DeviceClassName) {
        @Override
        public List<String> enumerateDevicePaths(
                Map<String, KeyRelease> deviceCache) {
            List<String> devicePaths = new ArrayList<String>();
            for (RemoteDevice remoteDevice : RemoteDevices
                    .devicesThatSupport(DeviceClassName)) {
                devicePaths.add(DeviceCache.createDevicePath(DeviceClassName,
                        remoteDevice.getDevicePath()));
            }
            return devicePaths;
        }

        @Override
        public KeyRelease createDevice(String deviceName) {
            if (WaitingForConnection.equals(deviceName)) {
                return new KeyRelease();
            } else {
                return new KeyRelease(
                        RemoteDevices.Instance.getDevice(deviceName));
            }
        }
    };

    public static final DeviceCache<KeyRelease> Devices = new DeviceCache<KeyRelease>()
            .addFactory(Factory);

    private static final String Actuators = "actuators";

    private RemoteDevice remoteDevice;
    private String[] releaseKeys = { "" };

    KeyRelease() {
        this(RemoteDevices.WaitingForConnection);
    }

    KeyRelease(RemoteDevice remoteDevice) {
        this.remoteDevice = remoteDevice;
    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(DeviceClassName,
                remoteDevice.getDevicePath());
    }

    @Override
    public String getName() {
        return remoteDevice.getName() + " " + remoteDevice.getServiceName()
                + " " + remoteDevice.getDescription() + " "
                + remoteDevice.getVersion();
    }

    @Override
    public boolean connected() {
        if (remoteDevice == RemoteDevices.WaitingForConnection) {
            List<RemoteDevice> keyReleaseDevices = RemoteDevices
                    .devicesThatSupport(DeviceClassName);
            if (!keyReleaseDevices.isEmpty()) {
                remoteDevice = keyReleaseDevices.get(0);
                Factory.connectDevice(this);
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

    public int actuators() {
        RemoteDeviceMessage count = remoteDevice.sendAndReceive(
                new RemoteDeviceMessage(DeviceClassName, Actuators));
        if (RemoteDevice.Count.equals(count.command)) {
            int actuators = Integer.parseInt(count.parameters.get(0));
            releaseKeys = new String[actuators];
            for (int i = 0; i < releaseKeys.length; i++) {
                releaseKeys[i] = "";
            }
            return actuators;
        } else {
            return 0;
        }
    }

    public int getBestActuatorForTime(int timeMinutes) {
        int actuatorCount = actuators();
        List<Integer> available = new ArrayList<Integer>(actuatorCount);
        for (int actuator = 0; actuator < actuatorCount; actuator++) {
            available.add(available(actuator));
        }
        return getBestActuator(timeMinutes, available);
    }

    static int getBestActuator(int timeMinutes,
            List<Integer> availableDurations) {
        int bestDifferenceSoFar = Integer.MAX_VALUE;
        int unset = Integer.MIN_VALUE;
        int bestActuator = unset;
        int maxDuration = unset;
        int maxActuator = unset;
        for (int actuator = 0; actuator < availableDurations
                .size(); actuator++) {
            int availableDuration = availableDurations.get(actuator);
            int difference = availableDuration - timeMinutes;
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

    public boolean arm(int actuator) {
        RemoteDeviceMessage ok = remoteDevice
                .sendAndReceive(new RemoteDeviceMessage(DeviceClassName, Arm,
                        Arrays.asList(Integer.toString(actuator))));
        releaseKeys[actuator] = "";
        return Ok.equals(ok.command);
    }

    public String start(int actuator, int timeMinutes) {
        RemoteDeviceMessage key = remoteDevice
                .sendAndReceive(new RemoteDeviceMessage(DeviceClassName, Start,
                        Arrays.asList(Integer.toString(actuator),
                                Integer.toString(timeMinutes))));
        if (ReleaseKey.equals(key.command)) {
            releaseKeys[actuator] = key.parameters.get(0);
            return releaseKeys[actuator];
        } else {
            return "";
        }
    }

    public int sleep(int timeMinutes) {
        return remoteDevice.sleep(timeMinutes);
    }

    public boolean add(int actuator, int minutes) {
        RemoteDeviceMessage ok = remoteDevice
                .sendAndReceive(new RemoteDeviceMessage(DeviceClassName, Add,
                        Arrays.asList(Integer.toString(actuator),
                                Integer.toString(minutes))));
        return Ok.equals(ok.command);
    }

    /**
     * Whether any of the actuators is holding a key.
     * 
     * @param actuator
     *            The actuator.
     * @return True if the actuator holds a key and is counting down.
     */
    public boolean isRunning(int actuator) {
        RemoteDeviceMessage count = remoteDevice
                .sendAndReceive(new RemoteDeviceMessage(DeviceClassName,
                        Running, Arrays.asList(Integer.toString(actuator))));
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
    public int available(int actuator) {
        RemoteDeviceMessage count = remoteDevice
                .sendAndReceive(new RemoteDeviceMessage(DeviceClassName,
                        Available, Arrays.asList(Integer.toString(actuator))));
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
    public int remaining(int actuator) {
        RemoteDeviceMessage count = remoteDevice
                .sendAndReceive(new RemoteDeviceMessage(DeviceClassName,
                        Remaining, Arrays.asList(Integer.toString(actuator))));
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
    public boolean release(int actuator) {
        RemoteDeviceMessage released = remoteDevice.sendAndReceive(
                new RemoteDeviceMessage(DeviceClassName, Release, Arrays.asList(
                        Integer.toString(actuator), releaseKeys[actuator])));
        return Ok.equals(released.command);
    }
}
