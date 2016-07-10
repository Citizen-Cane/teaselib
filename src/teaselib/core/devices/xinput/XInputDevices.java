/**
 * 
 */
package teaselib.core.devices.xinput;

import teaselib.core.devices.DeviceCache;

/**
 * @author someone
 *
 */
public class XInputDevices {
    public static final DeviceCache<XInputDevice> Instance = new DeviceCache<XInputDevice>() {

        @Override
        public XInputDevice getDefaultDevice() {
            String defaultId = getLast(getDevicePaths());
            return getDevice(defaultId);
        }
    }.addFactory(XInputDevice.Factory);
}
