package teaselib.core.devices;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeviceCache<T extends Device> {

    private static final String PathSeparator = "/";

    public interface Factory<T extends Device> {
        String getDeviceClass();

        List<String> getDevices();

        T getDevice(String path);
    }

    private final Map<String, Factory<T>> factories = new HashMap<String, Factory<T>>();

    public DeviceCache() {
    }

    public DeviceCache<T> addFactory(DeviceCache.Factory<T> factory) {
        factories.put(factory.getDeviceClass(), factory);
        return this;
    }

    private Map<String, T> devices = new LinkedHashMap<String, T>();

    public T getDefaultDevice() {
        String defaultId = getFirst(getDevicePaths());
        return getDevice(defaultId);
    }

    public static String getFirst(Collection<String> collection) {
        return collection.iterator().next();
    }

    public static String getLast(Collection<String> collection) {
        String s = null;
        Iterator<String> it = collection.iterator();
        while (it.hasNext()) {
            s = it.next();
        }
        return s;
    }

    public T getDevice(String devicePath) {
        if (devices.containsKey(devicePath)) {
            return devices.get(devicePath);
        } else {
            T device = create(devicePath);
            devices.put(devicePath, device);
            return device;
        }
    }

    private T create(String devicePath) {
        try {
            String deviceClassName = getDeviceClass(devicePath);
            T device = factories.get(deviceClassName).getDevice(devicePath);
            if (device == null) {
                throw new IllegalArgumentException(devicePath);
            }
            return device;
        } catch (Exception e) {
            throw new IllegalArgumentException(devicePath, e);
        }
    }

    public Set<String> getDevicePaths() {
        Set<String> devicePaths = new LinkedHashSet<String>();
        for (Map.Entry<String, Factory<T>> entry : factories.entrySet())
            devicePaths.addAll(entry.getValue().getDevices());
        return devicePaths;
    }

    public static String createDevicePath(String deviceClassName,
            String deviceName) {
        return deviceClassName + PathSeparator + deviceName;
    }

    public static String getDeviceName(String devicePath) {
        return devicePath.substring(devicePath.indexOf(PathSeparator) + 1);
    }

    public static String getDeviceClass(String devicePath) {
        return devicePath.substring(0, devicePath.indexOf(PathSeparator));
    }
}
