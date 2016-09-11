package teaselib.core.devices;

public interface DeviceFactoryListener<T extends Device> {
    void deviceCreated(T device);
}
