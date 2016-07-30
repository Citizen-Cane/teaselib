package teaselib.core.devices.remote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceCache;

public class KeyRelease implements Device {
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

    private static final String RemoteServiceName = "keyrelease";

    private RemoteDevice remoteDevice;

    KeyRelease(RemoteDevice remoteDevice) {
        this.remoteDevice = remoteDevice;
        int actuators = actuators();
        return;
    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(DeviceClassName,
                remoteDevice.getDevicePath());
    }

    @Override
    public String getName() {
        return remoteDevice.getName() + "Key Release";
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
                new RemoteDeviceMessage(DeviceClassName, "actuators"));
        // TODO error handling - don't throw
        return !"timeout".equals(count.command)
                ? Integer.parseInt(count.parameters.get(0)) : 0;
    }

    public boolean arm(int actuatorIndex) {
        RemoteDeviceMessage armed = remoteDevice
                .sendAndReceive(new RemoteDeviceMessage(DeviceClassName, "arm",
                        Arrays.asList(Integer.toString(actuatorIndex))));
        return !"timeout".equals(armed.command);
    }

    public int start(int actuatorIndex, int timeMinutes) {
        RemoteDeviceMessage durationMinutes = remoteDevice
                .sendAndReceive(new RemoteDeviceMessage(DeviceClassName,
                        "start", Arrays.asList(Integer.toString(actuatorIndex),
                                Integer.toString(timeMinutes))));
        return !"timeout".equals(durationMinutes.command)
                ? Integer.parseInt(durationMinutes.parameters.get(0)) : -1;
    }

    public boolean release(int actuatorIndex) {
        RemoteDeviceMessage released = remoteDevice.sendAndReceive(
                new RemoteDeviceMessage(DeviceClassName, "release",
                        Arrays.asList(Integer.toString(actuatorIndex))));
        return !"timeout".equals(released.command);
    }
}
