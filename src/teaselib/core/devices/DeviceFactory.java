package teaselib.core.devices;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import teaselib.core.Configuration;
import teaselib.core.ScriptInterruptedException;

public abstract class DeviceFactory<T extends Device> {
    private final String deviceClass;
    protected final Devices devices;
    protected final Configuration configuration;
    private final Map<String, T> deviceCache;

    public DeviceFactory(String deviceClass, Devices devices, Configuration configuration) {
        this.deviceClass = deviceClass;
        this.devices = devices;
        this.configuration = configuration;
        this.deviceCache = new LinkedHashMap<String, T>();
    }

    public String getDeviceClass() {
        return deviceClass;
    }

    public List<String> getDevices() {
        try {
            List<String> devicePaths = enumerateDevicePaths(deviceCache);
            if (devicePaths.isEmpty()) {
                devicePaths.add(DeviceCache.createDevicePath(deviceClass, Device.WaitingForConnection));
            }
            // remove disconnected
            Map<String, T> updatedDeviceCache = new LinkedHashMap<String, T>();
            for (String devicePath : devicePaths) {
                if (deviceCache.containsKey(devicePath)) {
                    updatedDeviceCache.put(devicePath, deviceCache.get(devicePath));
                }
            }
            deviceCache.clear();
            deviceCache.putAll(updatedDeviceCache);
            return devicePaths;
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException(e);
        }
    }

    public boolean isDeviceCached(String devicePath) {
        return deviceCache.containsKey(devicePath);
    }

    public T getDevice(String devicePath) {
        if (deviceCache.containsKey(devicePath)) {
            return deviceCache.get(devicePath);
        } else {
            String deviceName = DeviceCache.getDeviceName(devicePath);
            T device = createDevice(deviceName);
            deviceCache.put(devicePath, device);
            return device;
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
        String devicePath = device.getDevicePath();
        if (deviceCache.get(devicePath) == device) {
            deviceCache.remove(devicePath);
        }
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
