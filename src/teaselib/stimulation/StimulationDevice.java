package teaselib.stimulation;

import java.util.Collections;
import java.util.List;

import teaselib.core.configuration.Configuration;
import teaselib.core.devices.BatteryLevel;
import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;
import teaselib.core.devices.xinput.stimulation.XInputStimulationDevice;
import teaselib.stimulation.Stimulator.Output;
import teaselib.stimulation.Stimulator.Wiring;
import teaselib.stimulation.ext.StimulationTargets;

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

    // TODO Make this a device class to allow more control
    public static final StimulationDevice MANUAL = new StimulationDevice() {
        @Override
        public boolean isWireless() {
            return true;
        }

        @Override
        public String getName() {
            return "Manual EStim device";
        }

        @Override
        public String getDevicePath() {
            return "ManualEStimDevice/Mock";
        }

        @Override
        public boolean connected() {
            return true;
        }

        @Override
        public void close() {
            // Nothing to do
        }

        @Override
        public BatteryLevel batteryLevel() {
            return BatteryLevel.Medium;
        }

        @Override
        public boolean active() {
            return true;
        }

        @Override
        public void stop() {
            // Ignore
        }

        @Override
        public List<Stimulator> stimulators() {
            return Collections.emptyList();
        }

        @Override
        public void play(StimulationTargets targets) {
            // Ignore
        }

        @Override
        public void playAll(StimulationTargets targets) {
            // Ignore
        }

        @Override
        public void append(StimulationTargets targets) {
            // Ignore
        }

        @Override
        public void complete() {
            // Ignore
        }

        @Override
        public String toString() {
            return getDevicePath();
        }
    };

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

    public Output output = Output.EStim;
    public Wiring wiring = Wiring.INFERENCE_CHANNEL;

    public void setMode(Output output, Wiring wiring) {
        this.output = output;
        this.wiring = wiring;
    }

    public abstract List<Stimulator> stimulators();

    /**
     * Plays the targets in the current stream. Playing targets will be replaced, but targets on free chaneels will be
     * mixed in.
     * 
     * @param targets
     */
    public abstract void play(StimulationTargets targets);

    /**
     * Play all the targets, replacing any currently playing.
     * 
     * @param targets
     */
    public abstract void playAll(StimulationTargets targets);

    /**
     * Appends targets to the current stream. The current targets will be completed first, and the new ones seamlessly
     * appended without resetting any stimulation outputs.
     * <p>
     * THis method almost behaves like play(), complete(), play(), but without stopping the sampler thread or clearing
     * stimulation outputs.
     * 
     * @param targets
     */
    public abstract void append(StimulationTargets targets);

    public abstract void stop();

    public abstract void complete();
}
