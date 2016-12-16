/**
 * 
 */
package teaselib.core.devices.xinput;

import java.util.Set;

import teaselib.core.devices.DeviceCache;

/**
 * @author someone
 *
 */
public class XInputDevices {
    public static final DeviceCache<XInputDevice> Devices = new DeviceCache<XInputDevice>() {

        @Override
        public XInputDevice getDefaultDevice() {
            Set<String> devicePaths = getDevicePaths();
            for (String devicePath : devicePaths) {
                XInputDevice device = getDevice(devicePath);
                if (device.isWireless()) {
                    return device;
                }
            }
            String defaultId = getLast(devicePaths);
            return getDevice(defaultId);
        }
    }.addFactory(XInputDevice.Factory);
}
