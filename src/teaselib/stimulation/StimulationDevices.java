/**
 * 
 */
package teaselib.stimulation;

import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.xinput.stimulation.XInputStimulationDevice;

/**
 * @author someone
 * 
 *         todo map actions to motors and patterns
 * 
 *         actions: tease, punish, walk,run, etc. patterns are different
 *         depending on what is connected (and where)
 * 
 *         for instance: "tease tip estim" will be different from "tease tip
 *         vibrator" because of the different sensitivity of the area.
 * 
 *         Then there's the intensity of the stimulation: For a vibrator, you
 *         may want to render intensity by adjusting the motor voltage, but for
 *         controlling an e-stim current via a relay, the choice are on or off.
 *         So for relays, intensity maps to on/off time.
 * 
 * 
 * 
 */
public class StimulationDevices {

    public static final DeviceCache<StimulationDevice> Devices = new DeviceCache<StimulationDevice>() {

        @Override
        public StimulationDevice getDefaultDevice() {
            // Prefer wireless devices
            // (wireless XInput devices are usually numbered higher
            // since they aren't connected at boot time)
            return getDevice(getLast(getDevicePaths()));
        }
    }.addFactory(XInputStimulationDevice.Factory);
}
