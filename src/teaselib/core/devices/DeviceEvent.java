package teaselib.core.devices;

public interface DeviceEvent<T extends Device> {

    T getDevice();

}