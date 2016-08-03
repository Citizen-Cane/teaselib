package teaselib.core.devices.remote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceCache;

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
    private static final String Key = "key";

    /**
     * A numerical value, returned by the device
     */
    private static final String Count = "count";

    public static final DeviceCache<KeyRelease> Devices = new DeviceCache<KeyRelease>()
            .addFactory(new DeviceCache.Factory<KeyRelease>() {
                @Override
                public String getDeviceClass() {
                    return KeyRelease.DeviceClassName;
                }

                @Override
                public List<String> getDevices() {
                    List<String> devicePaths = new ArrayList<String>();
                    for (RemoteDevice remoteDevice : RemoteDevices
                            .devicesThatSupport(DeviceClassName)) {
                        devicePaths.add(DeviceCache.createDevicePath(
                                DeviceClassName, remoteDevice.getDevicePath()));
                    }
                    return devicePaths;
                }

                @Override
                public KeyRelease getDevice(String devicePath) {
                    final String deviceName = DeviceCache
                            .getDeviceName(devicePath);
                    RemoteDevice remoteDevice = RemoteDevices.Instance
                            .getDevice(deviceName);
                    return new KeyRelease(remoteDevice);
                }
            });

    private static final String DeviceClassName = "KeyRelease";

    private static final String Actuators = "actuators";

    private RemoteDevice remoteDevice;
    private String releaseKey;

    private String actuators;

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
        return remoteDevice.connected();
    }

    @Override
    public boolean active() {
        return remoteDevice.active();
    }

    @Override
    public void close() {
        remoteDevice.close();
    }

    public int actuators() {
        RemoteDeviceMessage count = remoteDevice.sendAndReceive(
                new RemoteDeviceMessage(DeviceClassName, Actuators));
        if (Count.equals(count.command)) {
            // TODO error handling - don't throw
            return Integer.parseInt(count.parameters.get(0));
        } else {
            return 0;
        }
    }

    public boolean arm(int actuatorIndex) {
        RemoteDeviceMessage ok = remoteDevice
                .sendAndReceive(new RemoteDeviceMessage(DeviceClassName, Arm,
                        Arrays.asList(Integer.toString(actuatorIndex))));
        return Ok.equals(ok.command);
    }

    public String start(int actuatorIndex, int timeMinutes) {
        RemoteDeviceMessage key = remoteDevice
                .sendAndReceive(new RemoteDeviceMessage(DeviceClassName, Start,
                        Arrays.asList(Integer.toString(actuatorIndex),
                                Integer.toString(timeMinutes))));
        if (Key.equals(key.command)) {
            releaseKey = key.parameters.get(0);
            return releaseKey;
        } else {
            return "";
        }
    }

    public boolean addTime(int actuatorIndex, int minutes) {
        RemoteDeviceMessage ok = remoteDevice
                .sendAndReceive(new RemoteDeviceMessage(DeviceClassName,
                        Release, Arrays.asList(Integer.toString(actuatorIndex),
                                Integer.toString(minutes))));
        return Ok.equals(ok.command);
    }

    public int remaining(int actuatorIndex) {
        RemoteDeviceMessage count = remoteDevice.sendAndReceive(
                new RemoteDeviceMessage(DeviceClassName, Remaining,
                        Arrays.asList(Integer.toString(actuatorIndex))));
        if (Count.equals(count.command)) {
            // TODO error handling - don't throw
            return Integer.parseInt(count.parameters.get(0));
        } else {
            return 0;
        }
    }

    public boolean release(int actuatorIndex) {
        RemoteDeviceMessage released = remoteDevice.sendAndReceive(
                new RemoteDeviceMessage(DeviceClassName, Release, Arrays
                        .asList(Integer.toString(actuatorIndex), releaseKey)));
        return Ok.equals(released.command);
    }
}
