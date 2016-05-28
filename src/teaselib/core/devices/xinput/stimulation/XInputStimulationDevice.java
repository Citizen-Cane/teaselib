package teaselib.core.devices.xinput.stimulation;

import java.util.ArrayList;
import java.util.List;

import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.xinput.XInputDevice;
import teaselib.stimulation.StimulationDevice;
import teaselib.stimulation.Stimulator;

/**
 * The XInputStimulator class turns any Microsoft XInput (x360, Xbox One and
 * compatible) controller into a vibrator or estim device.
 * 
 * <h1>To make a vibrator</h1> Disassemble the motors from the gamepad, extend
 * the cables and hot-glue them into a Kinder(tm)-eggshell. You can also control
 * any motorized sex-toy via the game controller by interfacing the motor cable.
 * Most sex toys work with two AA/AAA or a button cell at 3V, which is exactly
 * the voltage provided by the controllers' motor drivers.
 * 
 * The rough rumble motor (left, channel 0) is supposed to be used for
 * punishments.
 * 
 * The light rumble motor (right, channel 1) is supposed to be used for teasing.
 * 
 * <h1>To control an electro stimulation device</h1>
 * 
 * It is possible to control an estim device by connecting the motor output to a
 * reed relay. Now the xbox 360 controller runs on 3V, how the guaranteed
 * switching voltage of a 5V reed relay is usually around 3.8V (please consult
 * the data sheet).
 * <p>
 * However 3 out of 4 relays work with lower voltage (if not, try avoiding
 * rechargeable batteries) because the open-circuit voltage of batteries is
 * higher than the specified voltage. (up to 1.8V for 1.5-batteries
 * (2*1.8=3.6V), and because there won't be much load on the motor drivers when
 * connecting them to a reed relay, so we can expect the reed relay to be driven
 * with about 3.6v, which is just a bit below the guaranteed switching voltage.
 * <p>
 * The relay has to feature a protective diode to consume the current induced by
 * the solenoid of the relay. Otherwise that current might kill the vontroller's
 * motor driver. On second thought, the motor driver should have one already,
 * but reed releais with or without are the same price, so let's better be on
 * the safe side.
 * <p>
 * To prevent the e-stim device shutting itself off, connect a 10Kohm resistor
 * parallel to the reed relay. If the reed relay is open, the current of the
 * e-stm device will pass either through the resistor, making the estim device
 * think it's always connected.
 * 
 * <h1>Differences between vibrators and e-stim devices</h1>
 *
 * Motors do have some inertia. The rough rumble motor definitely needs about
 * 150ms to run at full speed.
 * <p>
 * When using reed relays, the shortest time the xinput driver allows seems to
 * be around 50-100ms
 * 
 * <h1>The timeout hack</h1>
 * 
 * Usually the wireless xbox controller shuts itself down when no button is
 * pressed for 15 minutes. For that reason it is a good idea to check whether
 * the stimulation device is still connected, and from time toi time instruct
 * the user to press a button.
 * 
 * However it's possible to add a hack to the controller to disable the buttons.
 * <h2>Removing the trigger potentiometers</h2> This is actually a prequisite to
 * replace the shoulder buttons.
 * <p>
 * The mechanical parts for turning the potentiometer of the triggers partially
 * cover the shoulder button micro-switches. So before unsoldering the switch,
 * the trigger has to be removed from the circuit board.
 * <p>
 * Well, I just wanted to remove the mechanical parts, but accidently pulled out
 * the potentiometers as well. Do the same, just be careful and do it slowly.
 * Afterwards, you may get strange readings for the triggers, because the sensor
 * pin of the trigger potio are now an all open circuit. Furthermore, when
 * enabling the motors or reed relays, the trigger values change a lot.
 * <p>
 * This alone may give you a disabled controller timeout - just tease your
 * victim before the timeout duration elapses to restart the timer.
 * 
 * <h2>Replace the shoulder buttons with a MOSFET</h2>
 * 
 * Unsolder the micro-switches. Get a MOSFET with a threshold voltage of 1-2V,
 * for instance a NDP6060L, or a IRLZ34N.
 * <p>
 * The pinout of the MOPS-FET in a TO-220 package is: Gate, Drain, Source. Both
 * micro-switches are connected with their right pin via 4-5K to GND, so solder
 * MOSFET Source to the right front hole, and MOSFET Drain to the left front
 * hole. Connect MOSFET Gate with the motor+ pin.
 * <p>
 * On my device, readings are not stable: When you run the timeout tests you
 * will notice that sometimes they fail. My guess here is that the actual
 * voltage that drives the MOSFET is somewhere between 1-2V. Also, other buttons
 * are detected as "Pressed" although they shouldn't.
 * <p>
 * The vibration drivers however work as intended and signal a button press each
 * time a stimulation is issued.
 */
public class XInputStimulationDevice implements StimulationDevice {
    public static final String DeviceClassName = "XInputStimulationDevice";

    private final XInputDevice device;
    private final List<Stimulator> stimulators;

    public XInputStimulationDevice(XInputDevice device) {
        super();
        this.device = device;
        this.stimulators = new ArrayList<Stimulator>(2);
        this.stimulators.addAll(XInputStimulator.getStimulators(this));
    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(DeviceClassName,
                device.getDevicePath());
    }

    @Override
    public String getName() {
        return "XInput stimulator";
    }

    @Override
    public boolean active() {
        return device.active();
    }

    @Override
    public void release() {
    }

    @Override
    public List<Stimulator> stimulators() {
        return stimulators;
    }

    XInputDevice getXInputDevice() {
        return device;
    }
}
