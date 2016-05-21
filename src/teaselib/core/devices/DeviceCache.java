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

    public interface DeviceFactory<T extends Device> {
        List<String> getDevices();

        T getDevice(String path);
    }

    private final Map<String, DeviceFactory<T>> factories = new HashMap<>();

    // public DeviceCache(Map.Entry<String, CreateDevice<T>>... creators) {
    // for (Map.Entry<String, CreateDevice<T>> entry : creators) {
    // this.creators.put(entry.getKey(), entry.getValue());
    // }
    // }

    public DeviceCache(String deviceClassName, DeviceFactory<T> factory) {
        factories.put(deviceClassName, factory);
    }

    private Map<String, T> devices = new LinkedHashMap<>();

    public T getDefaultDevice() {
        String defaultId = getFirst(getDevices());
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

    public Set<String> getDevices() {
        Set<String> devicePaths = new LinkedHashSet<>();
        for (Map.Entry<String, DeviceFactory<T>> entry : factories.entrySet())
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
