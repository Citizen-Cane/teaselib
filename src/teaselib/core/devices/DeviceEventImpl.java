package teaselib.core.devices;

public final class DeviceEventImpl<T extends Device> implements DeviceEvent<T> {
    private final DeviceCache<T> deviceCache;
    public final String devicePath;

    public DeviceEventImpl(DeviceCache<T> deviceCache, String devicePath) {
        this.deviceCache = deviceCache;
        this.devicePath = devicePath;
    }

    @Override
    public String getDevicePath() {
        return devicePath;
    }

    @Override
    public T getDevice() {
        return deviceCache.getDevice(devicePath);
    }
}