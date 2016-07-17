package teaselib.core.devices.remote;

import java.util.ArrayList;
import java.util.List;

import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.DeviceCache.Factory;

public class SelfBondageKeyRelease implements Device {
    public static final DeviceCache<SelfBondageKeyRelease> Devices = new DeviceCache<SelfBondageKeyRelease>()
            .addFactory(new DeviceCache.Factory<SelfBondageKeyRelease>() {
                @Override
                public String getDeviceClass() {
                    return SelfBondageKeyRelease.DeviceClassName;
                }

                @Override
                public List<String> getDevices() {
                    List<String> devicePaths = new ArrayList<String>();
                    for (RemoteDevice remoteDevice : RemoteDevices
                            .devicesThatSupport("SelfBondageTimelockDevice")) {
                        devicePaths.add(DeviceCache.createDevicePath(
                                DeviceClassName, remoteDevice.getDevicePath()));
                    }
                    return devicePaths;
                }

                @Override
                public SelfBondageKeyRelease getDevice(String devicePath) {
                    RemoteDevice remoteDevice = RemoteDevices.Instance
                            .getDevice(DeviceCache.getDeviceName(devicePath));
                    return new SelfBondageKeyRelease(remoteDevice);
                }
            });

    private static final String DeviceClassName = "SelfBondageTimelockDevice";

    private RemoteDevice remoteDevice;

    SelfBondageKeyRelease(RemoteDevice remoteDevice) {
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
    public boolean active() {
        return remoteDevice.active();
    }

    @Override
    public void release() {
        remoteDevice.release();
    }

}
