package teaselib.core.devices;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import teaselib.core.ScriptInterruptedException;

public class DeviceCache<T extends Device> {
    private static final int CONNECTION_WAIT_UNTIL_CONNECTED = -1;
    private final Map<String, DeviceFactory<? extends T>> factories = new LinkedHashMap<>();
    private final Set<DeviceFactoryListener<T>> deviceListeners = new LinkedHashSet<>();

    private static final char PathSeparator = '/';

    public static final class DeviceNotFoundException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        public DeviceNotFoundException(String message) {
            super(message);
        }

        public DeviceNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public DeviceCache<T> addFactory(DeviceFactory<? extends T> factory) {
        factories.put(factory.getDeviceClass(), factory);
        return this;
    }

    private Map<String, T> devices = new LinkedHashMap<>();

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
            fireDeviceCreated(device);
            return device;
        }
    }

    private T create(String devicePath) {
        try {
            String deviceClassName = getDeviceClass(devicePath);
            DeviceFactory<? extends T> deviceFactory = factories.get(deviceClassName);
            if (deviceFactory == null) {
                throw new DeviceNotFoundException("Factory for" + deviceClassName + " not found");
            }
            T device = deviceFactory.getDevice(devicePath);
            if (device == null) {
                throw new DeviceNotFoundException(devicePath);
            }
            return device;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new DeviceNotFoundException(devicePath, e);
        }
    }

    public Set<String> getDevicePaths() {
        Set<String> devicePaths = new LinkedHashSet<>();
        for (Map.Entry<String, DeviceFactory<? extends T>> entry : factories.entrySet())
            devicePaths.addAll(entry.getValue().getDevices());
        return devicePaths;
    }

    public static String createDevicePath(String deviceClassName, String deviceName) {
        return deviceClassName + PathSeparator + deviceName;
    }

    public static String getDeviceName(String devicePath) {
        return devicePath.substring(devicePath.indexOf(PathSeparator) + 1);
    }

    public static String getDeviceClass(String devicePath) {
        return devicePath.substring(0, devicePath.indexOf(PathSeparator));
    }

    public static String getParentDevice(String devicePath) {
        return devicePath.substring(0, devicePath.lastIndexOf(PathSeparator));
    }

    public static boolean connect(Device device) {
        return connect(device, CONNECTION_WAIT_UNTIL_CONNECTED);
    }

    /**
     * Poll for connection until the device is connected or the timeout duration is exceeded.
     * <p>
     * The poll frequency depends on the the runtime duration of each connect call, but will be at least 1 second.
     * 
     * @param device
     *            The device to connect
     * @param timeoutSeconds
     *            The maximum time to poll.
     * @return True if the device is connected.
     * @throws ScriptInterruptedException
     */
    public static boolean connect(Device device, double timeoutSeconds) throws ScriptInterruptedException {
        long timeoutMillis = timeoutSeconds >= 0 ? (long) timeoutSeconds * 1000 : Long.MAX_VALUE;
        while (timeoutMillis >= 0) {
            long now = System.currentTimeMillis();
            if (device.connected()) {
                return true;
            }
            long elapsed = System.currentTimeMillis() - now;
            if (elapsed > timeoutMillis) {
                return false;
            }
            long duration = Math.min(timeoutMillis, Math.max(1000, elapsed * 10));
            try {
                Thread.sleep(duration);
                timeoutMillis -= duration;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ScriptInterruptedException(e);
            }
        }
        return device.connected();
    }

    private void fireDeviceCreated(T device) {
        for (DeviceFactoryListener<T> deviceListener : deviceListeners) {
            deviceListener.deviceCreated(device);
        }
    }

    public void addDeviceListener(DeviceFactoryListener<T> deviceListener) {
        deviceListeners.add(deviceListener);
    }

    public void removeDeviceListener(DeviceFactoryListener<T> deviceListener) {
        deviceListeners.remove(deviceListener);
    }

}