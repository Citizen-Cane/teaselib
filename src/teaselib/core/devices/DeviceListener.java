package teaselib.core.devices;

public interface DeviceListener<T extends Device> {
    void deviceConnected(DeviceEvent<T> e);

    void deviceDisconnected(DeviceEvent<T> e);
}
