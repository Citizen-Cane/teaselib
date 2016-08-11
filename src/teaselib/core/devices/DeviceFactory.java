package teaselib.core.devices;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class DeviceFactory<T extends Device> {
    private final String deviceClass;

    private final Map<String, T> deviceCache;

    public DeviceFactory(String deviceClass) {
        this.deviceClass = deviceClass;
        this.deviceCache = new LinkedHashMap<String, T>();
    }

    public String getDeviceClass() {
        return deviceClass;
    }

    public List<String> getDevices() {
        List<String> devicePaths = enumerateDevicePaths(deviceCache);
        if (devicePaths.isEmpty()) {
            devicePaths.add(Device.WaitingForConnection);
        }
        // TODO sync with device map: remove non-existing, add new
        return devicePaths;
    }

    public T getDevice(String devicePath) {
        if (deviceCache.containsKey(devicePath)) {
            return deviceCache.get(devicePath);
        } else {
            T device = createDevice(DeviceCache.getDeviceName(devicePath));
            deviceCache.put(devicePath, device);
            return device;
        }
    }

    /**
     * Notify the factory that a device has successfully (re-)connected to an
     * entity, and therefore its device path has changed.
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
     * Disconnect a device to waiting state. Devices usually only reconnect to
     * the entity denoted by the device path, but sometimes it may be desirable
     * or necessary to connect to any connected entity.
     * 
     * @param device
     */
    public String disconnectDevice(T device) {
        if (deviceCache.get(device.getDevicePath()) == device) {
            deviceCache.remove(device.getDevicePath());
        }
        deviceCache.put(Device.WaitingForConnection, device);
        return Device.WaitingForConnection;
    }

    /**
     * Enumerate device paths
     * 
     * @param deviceCache
     *            Cache devices if already constructed during enumeration
     * @return A list of device paths
     */
    public abstract List<String> enumerateDevicePaths(
            Map<String, T> deviceCache);

    /**
     * Create a new device.
     * 
     * @param devicePath
     *            The device path of the device, or
     *            {@link Device#WaitingForConnection} to create a device that is
     *            going to be connected later on.
     * @return A device instance that corresponds to the given device name.
     */
    public abstract T createDevice(String deviceName);
}
