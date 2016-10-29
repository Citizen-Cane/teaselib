/**
 * 
 */
package teaselib.core.devices.remote;

/**
 * @author someone
 *
 */
public interface RemoteDeviceListener {
    void deviceAdded(String name, String address, String serviceName,
            String description, String version);
}
