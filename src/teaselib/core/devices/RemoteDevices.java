package teaselib.core.devices;

import java.util.List;
import java.util.Vector;

public class RemoteDevices {

    public static final DeviceCache<RemoteDevice> Instance = new DeviceCache<RemoteDevice>()
            .addFactory(LocalNetworkDevice.Factory).addFactory(BluetoothDevice.Factory);

    public static List<RemoteDevice> devicesThatSupport(String serviceName) {
        List<RemoteDevice> remoteDevices = new Vector<RemoteDevice>();
        for (String devicePath : Instance.getDevicePaths())
            if (serviceName
                    .equals(Instance.getDevice(devicePath).getServiceName())) {
                remoteDevices.add(Instance.getDevice(devicePath));
            }
        return remoteDevices;
    }

}
