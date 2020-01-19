package teaselib.core.devices;

public interface DeviceFactoryListener<T extends Device> {
    void deviceConnected(DeviceEvent<T> e);

    void deviceDisconnected(DeviceEvent<T> e);
}
