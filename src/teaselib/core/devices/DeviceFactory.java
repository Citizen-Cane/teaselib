package teaselib.core.devices;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import teaselib.core.configuration.Configuration;

public abstract class DeviceFactory<T extends Device> {
    private final String deviceClassName;
    protected final Devices devices;
    protected final Configuration configuration;

    private final Map<String, Supplier<T>> discovered = new HashMap<>();
    private final Map<String, T> deviceCache = new LinkedHashMap<>();

    public DeviceFactory(String deviceClassName, Devices devices, Configuration configuration) {
        this.deviceClassName = deviceClassName;
        this.devices = devices;
        this.configuration = configuration;
    }

    public String getDeviceClass() {
        return deviceClassName;
    }

    public List<String> getDevices() {
        List<String> devicePaths;
        try {
            devicePaths = enumerateDevicePaths(deviceCache);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        }

        if (devicePaths.isEmpty()) {
            devicePaths.add(DeviceCache.createDevicePath(deviceClassName, Device.WaitingForConnection));
        }

        // remove disconnected
        // TODO call removed event for each removed device
        Map<String, T> updatedDeviceCache = new LinkedHashMap<>();
        for (String devicePath : devicePaths) {
            if (deviceCache.containsKey(devicePath)) {
                updatedDeviceCache.put(devicePath, deviceCache.get(devicePath));
            }
        }

        deviceCache.clear();
        deviceCache.putAll(updatedDeviceCache);

        devicePaths.addAll(discovered.keySet());
        return devicePaths;
    }

    public boolean isDeviceCached(String devicePath) {
        return deviceCache.containsKey(devicePath);
    }

    public T getDevice(String devicePath) {
        if (deviceCache.containsKey(devicePath)) {
            return deviceCache.get(devicePath);
        } else {
            if (discovered.containsKey(devicePath)) {
                return createDiscoveredDevice(devicePath);
            } else {
                return createFromDevicePath(devicePath);
            }
        }
    }

    private T createDiscoveredDevice(String devicePath) {
        T device = discovered.remove(devicePath).get();
        deviceCache.put(devicePath, device);
        return device;
    }

    private T createFromDevicePath(String devicePath) {
        String deviceName = DeviceCache.getDeviceName(devicePath);
        T device = createDevice(deviceName);
        deviceCache.put(devicePath, device);
        return device;
    }

    public void removeDevice(String devicePath) {
        if (deviceCache.containsKey(devicePath)) {
            deviceCache.remove(devicePath);
        } else {
            throw new NoSuchElementException(devicePath);
        }
    }

    /**
     * Notify the factory that a device has successfully (re-)connected to an entity, and therefore its device path has
     * changed.
     * 
     * @param device
     *            The device to update in the cache.
     */
    public void connectDevice(T device) {
        if (deviceCache.get(Device.WaitingForConnection) == device) {
            deviceCache.remove(Device.WaitingForConnection);
        }
        deviceCache.put(device.getDevicePath(), device);
    }

    /**
     * Remove a disconnect a device from the device cache after surprise-removal. The device is removed from the cache.
     * The device instance may reconnect later on by calling {@link DeviceFactory#connectDevice(Device)}
     * 
     * @param device
     *            The disconnected device.
     */
    public void removeDisconnectedDevice(T device) {
        removeDevice(device.getDevicePath());
    }

    protected void deviceDiscovered(String devicePath, Supplier<T> device) {
        discovered.put(devicePath, device);
    }

    protected void fireDeviceConnected(String devicePath, Class<T> deviceClass) {
        devices.get(deviceClass).fireDeviceConnected(devicePath);
    }

    protected void fireDeviceDisconnected(String devicePath, Class<T> deviceClass) {
        devices.get(deviceClass).fireDeviceDisconnected(devicePath);
    }

    /**
     * Enumerate device paths
     * 
     * @param deviceCache
     *            Cache devices if already constructed during enumeration
     * @return A list of device paths
     */
    public abstract List<String> enumerateDevicePaths(Map<String, T> deviceCache) throws InterruptedException;

    /**
     * Create a new device.
     * 
     * @param devicePath
     *            The device path of the device, or {@link Device#WaitingForConnection} to create a device that is going
     *            to be connected later on.
     * @return A device instance that corresponds to the given device name.
     */
    public abstract T createDevice(String deviceName);
}
