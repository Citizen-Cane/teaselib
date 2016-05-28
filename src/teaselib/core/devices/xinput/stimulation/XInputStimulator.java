/**
 * 
 */
package teaselib.core.devices.xinput.stimulation;

import java.util.ArrayList;
import java.util.List;

import teaselib.core.devices.xinput.XInputDevice;
import teaselib.stimulation.StimulationDevice;
import teaselib.stimulation.Stimulator;

public class XInputStimulator implements Stimulator {

    static class SharedState {
        final XInputDevice device;
        int leftMotor = 0;
        int rightMotor = 0;

        public SharedState(XInputDevice device) {
            super();
            this.device = device;
        }

        public void setLeftMotor(int value) {
            leftMotor = value;
            device.setVibration(leftMotor, rightMotor);
        }

        public void setRightMotor(int value) {
            rightMotor = value;
            device.setVibration(leftMotor, rightMotor);
        }
    }

    final StimulationDevice device;
    final SharedState sharedState;
    final int channel;

    public ChannelDependency channelDependency = ChannelDependency.Independent;
    public Output output = Output.Vibration;

    /**
     * Get all stimulators for a device.
     * 
     * @param device
     *            An XInput game controller
     * @return The stimulators share the device, because the outputs can only be
     *         set simultaneously
     */
    public static List<XInputStimulator> getStimulators(
            XInputStimulationDevice device) {
        List<XInputStimulator> stimulators = new ArrayList<XInputStimulator>(2);
        XInputStimulator channel0 = new XInputStimulator(device, 0);
        stimulators.add(channel0);
        stimulators.add(new XInputStimulator(channel0, 1));
        return stimulators;
    }

    XInputStimulator(XInputStimulationDevice device, int channel) {
        this.device = device;
        this.sharedState = new SharedState(device.getXInputDevice());
        this.channel = channel;
    }

    XInputStimulator(XInputStimulator sibling, int channel) {
        this.device = sibling.device;
        this.sharedState = sibling.sharedState;
        this.channel = channel;
    }

    @Override
    public void set(double value) {
        value = Math.max(0.0, value);
        value = Math.min(value, 1.0);
        int strength = (int) (value * XInputDevice.VIBRATION_MAX_VALUE);
        if (channel == 0) {
            sharedState.setLeftMotor(strength);
        } else {
            sharedState.setRightMotor(strength);
        }
    }

    @Override
    public String getDeviceName() {
        return "XBox Gamepad " + sharedState.device.getPlayerNum() + " "
                + (channel == 0 ? "Left" : "Right") + " channel";
    }

    @Override
    public String getLocation() {
        return channel == 0 ? "Left rumble motor" : "Right rumble motor";
    }

    @Override
    public StimulationDevice getDevice() {
        return device;
    }

    @Override
    public ChannelDependency channelDependency() {
        return channelDependency;
    }

    @Override
    public Output output() {
        return output;
    }

    @Override
    public double minimalSignalDuration() {
        return output == Output.Vibration ? 0.15 : 0.05;
    }

}
