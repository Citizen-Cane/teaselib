package teaselib.core.devices.remote;

import java.util.ArrayList;
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
                    RemoteDevice remoteDevice = RemoteDevices.Instance
                            .getDevice(DeviceCache.getDeviceName(devicePath));
                    return new KeyRelease(remoteDevice);
                }
            });

    private static final String DeviceClassName = "KeyRelease";

    private RemoteDevice remoteDevice;

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

}
