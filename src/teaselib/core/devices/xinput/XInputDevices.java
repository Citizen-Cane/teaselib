/**
 * 
 */
package teaselib.core.devices.xinput;

import java.util.ArrayList;
import java.util.List;

import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.DeviceCache.DeviceFactory;

/**
 * @author someone
 *
 */
public class XInputDevices {
    public static final DeviceCache<XInputDevice> Instance = new DeviceCache<XInputDevice>(
            XInputDevice.DeviceClassName, new DeviceFactory<XInputDevice>() {
                @Override
                public List<String> getDevices() {
                    List<String> deviceNames = new ArrayList<String>();
                    deviceNames.addAll(XInputDevice.getDevicePaths());
                    return deviceNames;
                }

                @Override
                public XInputDevice getDevice(String path) {
                    return XInputDevice.getDeviceFor(
                            Integer.parseInt(DeviceCache.getDeviceName(path)));
                }
            });
}
