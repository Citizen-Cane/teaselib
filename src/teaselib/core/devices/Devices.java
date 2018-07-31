package teaselib.core.devices;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import teaselib.core.configuration.Configuration;

public class Devices {
    private final Map<Class<?>, DeviceCache<? extends Device>> deviceClasses = new HashMap<Class<?>, DeviceCache<?>>();
    private Object configuration;

    public Devices(Object configuration) {
        this.configuration = configuration;
    }

    public <T extends Device.Creatable> DeviceCache<T> get(Class<T> deviceClass) {
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
                return deviceCache;
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
