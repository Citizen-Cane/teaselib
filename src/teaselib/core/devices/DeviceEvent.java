package teaselib.core.devices;

public final class DeviceEvent<T extends Device> {
    private final DeviceCache<T> deviceCache;
    public final String devicePath;

    public DeviceEvent(DeviceCache<T> deviceCache, String devicePath) {
        this.deviceCache = deviceCache;
        this.devicePath = devicePath;
    }

    public T getDevice() {
        return deviceCache.getDevice(devicePath);
    }
}