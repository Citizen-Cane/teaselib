package teaselib.stimulation;

import java.util.List;

import teaselib.core.Configuration;
import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;
import teaselib.core.devices.xinput.stimulation.XInputStimulationDevice;
import teaselib.stimulation.Stimulator.Output;
import teaselib.stimulation.Stimulator.Wiring;
import teaselib.stimulation.ext.StimulationChannels;

/**
 * 
 * TODO map actions to motors and patterns
 * 
 * actions: tease, punish, walk,run, etc. patterns are different depending on what is connected (and where)
 * 
 * for instance: "tease tip estim" will be different from "tease tip vibrator" because of the different sensitivity of
 * the area.
 * 
 * Then there's the intensity of the stimulation: For a vibrator, you may want to render intensity by adjusting the
 * motor voltage, but for controlling an e-stim current via a relay, the choice are on or off. So for relays, intensity
 * maps to on/off time.
 * 
 * @author Citizen-Cane
 */
public abstract class StimulationDevice implements Device.Creatable {

    public static synchronized DeviceCache<StimulationDevice> getDeviceCache(Devices devices,
            Configuration configuration) {
        return new DeviceCache<StimulationDevice>() {
            @Override
            public StimulationDevice getDefaultDevice() {
                // Prefer wireless devices
                // (wireless XInput devices are usually numbered higher
                // since they aren't connected at boot time)
                // TODO Check wireless status
                return getDevice(getLast(getDevicePaths()));
            }
        }.addFactory(XInputStimulationDevice.getDeviceFactory(devices, configuration));
    }

    public Output output = Output.EStim;;
    public Wiring wiring = Wiring.CommonGround_Accumulation;

    public void setMode(Output output, Wiring wiring) {
        this.output = output;
        this.wiring = wiring;
    }

    public abstract List<Stimulator> stimulators();

    public abstract void play(StimulationChannels channels, int repeatCount);

    public abstract void stop();

    public abstract void complete();
}
