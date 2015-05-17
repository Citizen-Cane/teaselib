/**
 * 
 */
package teaselib.stimulation;

import teaselib.devices.xinput.XInputDevice;

/* @author someone
 *         The XInputStimulator class turns any Microsoft XInput (x360c, xbox
 *         one and compatible) controller into a vibrator. Works best if you
 *         disassemble the motors from the gamepad, extend the cables and
 *         hot-glue them into a Kinderegg.
 * 
 *         The rough rumble motor (left, stim channel 0) is supposed to be used for
 *         punishments
 * 
 *         The light rumble motor (right, stim channel 1) is supposed to be used for
 *         teasing
 * 
 *         It is possible to control an estim device by connecting the motor
 *         output to a reed relay. The xbox 360 controller runs on 3V, but the
 *         guaranteed switching voltage of a 5V reed relay is usually around
 *         3.8V (consult the data sheet). However 3 out of 4 relays worked (if
 *         not, try avoiding rechargeable batteries) because the open-circuit
 *         voltage of batteries is more than the specified voltage. (up to 1.8V
 *         for 1.5-batteries (2*1.8=3.6V), and there's no load on the motor
 *         drivers when connecting it to a reed relay, we can expect the reed
 *         relay to be driven with about 3.6v, which is just a bit below the
 *         guaranteed switching voltage.
 * 
 *         The relay has to feature a diode to consume the current induced by
 *         the solenoid of the relay. Otherwise that current might kill the
 *         motor driver of the controller. On second thought, the motor driver
 *         should have one already. But they're the same price, so better be on
 *         the safe side.
 * 
 *         You have to connect a 10Kohm resistor parallel to the reed relay to
 *         avoid the e-stim device shutting off. The current will then pass
 *         either through the resistor or through the relay.
 *         
 *         Motors do have some intertia.
 *         
 *         The rough rumble motor definitely needs about 150ms to run at full speed.
 *         
 *          When using relays, the shortest time the xinput driver allows seems to be around 50-100ms
 *           
 */
public class XInputStimulator implements Stimulator {

    static class SharedState {
        final XInputDevice device;
        short leftMotor = 0;
        short rightMotor = 0;

        public SharedState(XInputDevice device) {
            super();
            this.device = device;
        }

        public void setLeftMotor(short value) {
            leftMotor = value;
            device.setVibration(leftMotor, rightMotor);
        }

        public void setRightMotor(short value) {
            leftMotor = value;
            device.setVibration(leftMotor, rightMotor);
        }
    }

    final SharedState sharedState;

    final int channel;

    public XInputStimulator(SharedState sharedState, int channel) {
        this.sharedState = sharedState;
        this.channel = channel;
    }

    @Override
    public void set(double value) {
        value = Math.max(0.0, value);
        value = Math.min(value, 1.0);
        short strength = (short) (value * 65535);
        if (channel == 0) {
            sharedState.setLeftMotor(strength);
        } else {
            sharedState.setRightMotor(strength);
        }
    }

}
