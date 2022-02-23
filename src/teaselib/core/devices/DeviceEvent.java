package teaselib.core.devices;

public interface DeviceEvent<T extends Device> {

    String getDevicePath();

    T getDevice();

}
