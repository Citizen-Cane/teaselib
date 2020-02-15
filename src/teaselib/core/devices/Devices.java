package teaselib.core.devices;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import teaselib.core.configuration.Configuration;
import teaselib.core.devices.remote.RemoteDevice;

public class Devices {
    private final Map<Class<?>, DeviceCache<? extends Device>> deviceClasses = new HashMap<>();
    private final Map<String, DeviceCache<? extends Device>> deviceClassNames = new HashMap<>();
    private final Configuration configuration;

    public Devices(Configuration configuration) {
        this.configuration = configuration;
    }

    public <T extends Device> DeviceCache<T> get(Class<T> deviceClass) {
        if (deviceClasses.containsKey(deviceClass)) {
            @SuppressWarnings("unchecked")
            DeviceCache<T> deviceCache = (DeviceCache<T>) deviceClasses.get(deviceClass);
            return deviceCache;
        } else {
            try {
                Method method = deviceClass.getMethod("getDeviceCache", Devices.class, Configuration.class);
                @SuppressWarnings("unchecked")
                DeviceCache<T> deviceCache = (DeviceCache<T>) method.invoke(this.getClass(), this, configuration);
                deviceClasses.put(deviceClass, deviceCache);
                deviceCache.getFactoryClassNames().stream().forEach(name -> deviceClassNames.put(name, deviceCache));
                return deviceCache;
            } catch (ReflectiveOperationException e) {
                throw new UnsupportedOperationException(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Device> T get(String devicePath) {
        String deviceClass = DeviceCache.getDeviceClass(devicePath);
        DeviceCache<? extends Device> deviceCache = deviceClassNames.get(deviceClass);
        return (T) deviceCache.getDevice(devicePath);
    }

    public <T extends RemoteDevice> List<T> thatSupport(String serviceName, Class<T> deviceClass) {
        List<T> remoteDevices = new ArrayList<>();
        DeviceCache<T> cache = get(deviceClass);
        for (String devicePath : cache.getDevicePaths()) {
            if (!Device.WaitingForConnection.equals(DeviceCache.getDeviceName(devicePath))) {
                T device = cache.getDevice(devicePath);
                if (serviceName.equals(device.getServiceName())) {
                    remoteDevices.add(device);
                }
            }
        }
        return remoteDevices;
    }

    public <T extends Device> T getDefaultDevice(Class<T> deviceClass) {
        DeviceCache<T> deviceCache = get(deviceClass);
        return deviceCache.getDefaultDevice();
    }

}
