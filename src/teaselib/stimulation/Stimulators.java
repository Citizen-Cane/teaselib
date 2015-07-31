/**
 * 
 */
package teaselib.stimulation;

import java.util.List;
import java.util.Vector;

import teaselib.core.devices.xinput.XInputDevice;
import teaselib.core.devices.xinput.XInputStimulator;

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
public class Stimulators {

    /**
     * Enumerates all stimulators. If a device contains multiple stimulators,
     * the are enumerated in right to left sequence as located on the device
     * (position on device as indicated by buttons, connectors, etc).
     * 
     * @return
     */
    public static List<Stimulator> getAll() {
        Vector<Stimulator> all = new Vector<Stimulator>();
        XInputDevice[] devices = XInputDevice.getAllDevices();
        for (XInputDevice device : devices) {
            if (device.isConnected()) {
                all.addAll(XInputStimulator.getStimulators(device));
            }
        }
        return all;
    }
}
