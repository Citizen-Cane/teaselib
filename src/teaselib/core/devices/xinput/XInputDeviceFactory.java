package teaselib.core.devices.xinput;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import teaselib.core.Configuration;
import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceFactory;
import teaselib.core.devices.Devices;

final class XInputDeviceFactory extends DeviceFactory<XInputDevice> {
    XInputDeviceFactory(String deviceClass, Devices devices, Configuration configuration) {
        super(deviceClass, devices, configuration);
    }

    @Override
    public List<String> enumerateDevicePaths(Map<String, XInputDevice> deviceCache) {
        List<String> deviceNames = new ArrayList<>();
        deviceNames.addAll(XInputDevice.getDevicePaths());
        return deviceNames;
    }

    @Override
    public XInputDevice createDevice(String deviceName) {
        if (Device.WaitingForConnection.equals(deviceName)) {
            return XInputDevice.getDeviceFor(0);
        } else {
            return XInputDevice.getDeviceFor(Integer.parseInt(deviceName));
        }
    }
}