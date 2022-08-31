package teaselib.test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import teaselib.core.configuration.Configuration;
import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceFactory;
import teaselib.core.devices.DeviceNotFoundException;
import teaselib.core.devices.Devices;

public final class TestDeviceFactory<T extends Device> extends DeviceFactory<T> {
    private T testDevice;

    public TestDeviceFactory(String deviceClass, Devices devices, Configuration configuration) {
        super(deviceClass, devices, configuration);
    }

    public void setTestDevice(T testDevice) {
        this.testDevice = testDevice;
    }

    @Override
    public List<String> enumerateDevicePaths(Map<String, T> deviceCache) throws InterruptedException {
        return Collections.singletonList(testDevice.getDevicePath());
    }

    @Override
    public T createDevice(String deviceName) {
        if (!deviceName.equals(testDevice.getDevicePath())) {
            throw new DeviceNotFoundException(deviceName);
        }
        return testDevice;
    }
}