package teaselib.core.devices.remote;

import teaselib.core.devices.Device;

public interface RemoteDevice extends Device {

    String getServiceName();

    String getVersion();
}
