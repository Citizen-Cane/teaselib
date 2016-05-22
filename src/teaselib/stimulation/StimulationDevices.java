/**
 * 
 */
package teaselib.stimulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.xinput.XInputDevice;
import teaselib.core.devices.xinput.XInputDevices;
import teaselib.core.devices.xinput.stimulation.XInputStimulationDevice;

/**
 * @author someone
 * 
 *         todo map actions to motors and patterns
 * 
 *         actions: tease, punish, walk,run, etc. patterns are different
 *         depending on what is connected (and where)
 * 
 *         for instance: "tease tip estim" will be different from
 *         "tesase tip vibrator" because of the different sensitivity of the
 *         area.
 * 
 *         Then there's the intensity of the stimulation: For a vibrator, you
 *         may want to render intensity by adjusting the motor voltage, but for
 *         controlling an e-stim current via a relay, the choice are on or off.
 *         So for relays, intensity maps to on/off time.
 * 
 * 
 * 
 */
public class StimulationDevices extends DeviceCache<StimulationDevice> {

    public static final DeviceCache<StimulationDevice> Instance = new StimulationDevices();

    private StimulationDevices() {
        super(XInputStimulationDevice.DeviceClassName,
                new DeviceCache.DeviceFactory<StimulationDevice>() {
                    @Override
                    public List<String> getDevices() {
                        List<String> deviceNames = new ArrayList<String>(4);
                        for (String devicePath : XInputDevice
                                .getDevicePaths()) {
                            XInputDevice device = XInputDevices.Instance
                                    .getDevice(devicePath);
                            if (device.isConnected()) {
                                deviceNames.add(createDevicePath(
                                        XInputStimulationDevice.DeviceClassName,
                                        device.getDevicePath()));
                            }
                        }
                        return deviceNames;
                    }

                    @Override
                    public XInputStimulationDevice getDevice(
                            String devicePath) {
                        return new XInputStimulationDevice(
                                XInputDevices.Instance
                                        .getDevice(getDeviceName(devicePath)));
                    }
                });
    }

    @Override
    public StimulationDevice getDefaultDevice() {
        // Prefer wireless devices
        // (wireless XInput devices are numbered higher
        // since they usually aren't connected at boot time)
        return getDevice(getLast(getDevices()));
    }

    @Override
    public StimulationDevice getDevice(String devicePath) {
        return super.getDevice(devicePath);
    }

    @Override
    public Set<String> getDevices() {
        return super.getDevices();
    }

}
