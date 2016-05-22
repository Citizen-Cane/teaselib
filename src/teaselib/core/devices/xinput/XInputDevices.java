/**
 * 
 */
package teaselib.core.devices.xinput;

import java.util.ArrayList;
import java.util.List;

import teaselib.core.devices.DeviceCache;

/**
 * @author someone
 *
 */
public class XInputDevices extends DeviceCache<XInputDevice> {
    public static final DeviceCache<XInputDevice> Instance = new XInputDevices(
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
                            Integer.parseInt(getDeviceName(path)));
                }
            });

    public XInputDevices(String deviceClassName,
            teaselib.core.devices.DeviceCache.DeviceFactory<XInputDevice> factory) {
        super(deviceClassName, factory);
    }

}
