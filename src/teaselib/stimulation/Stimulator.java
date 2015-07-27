/**
 * 
 */
package teaselib.stimulation;

/**
 * Represents a stimulator device channel.
 * 
 * A device may consists of multiple stimulators (like gamepads), in that case
 * several stimulator instances are created for the device.
 * 
 * @author someone
 *
 */
public interface Stimulator {

    /**
     * The extent to which the channels of a device can be contolled
     * independetly from each other:
     * 
     * XBox gamepad with rumble motors: independent as long as there is enough
     * power
     * 
     * XBox gamepad with estim hack:
     * 
     * independent as long if the voltage is high enough (batteries instead of
     * accumulators, good reed relais, etc.), and the estim device is also
     * independent
     * 
     * Partially independent if reed relays need a somewhat higher trigger
     * voltage, or the estim unit has multiple channels but they are not
     * completely isolated from each other
     * 
     * Dependent: Single channel estim device is used to power both gamepad
     * channels
     * 
     * @author someone
     *
     */
    enum ChannelDependency {
        Independent,
        PartiallyDependent,
        Dependent
    }

    /**
     * The physical result of the stimulation
     * 
     * @author someone
     *
     */
    enum Output {
        Vibration,
        EStim
    }

    /**
     * Stimulation strength, as of now analog for vibration, but digital for
     * estim
     * 
     * @param value
     *            The stimulation value from 0.0-1.0
     */
    void set(double value);

    /**
     * The name of the device that provides the stimulation
     * 
     * @return
     */
    String getDeviceName();

    /**
     * Location of the stimulator on the device
     * 
     * @return
     */
    String getLocation();

    /**
     * The device that controls the stimulation output
     * 
     * @return
     */
    Object getDevice();

    /**
     * Whether the channel is independent from the other channels of the device
     * 
     * @return
     */
    ChannelDependency channelDependency();

    /**
     * The kind of output this stimulator delivers
     * 
     * @return
     */
    Output output();

    /**
     * The duration the output value must be set to high in order to receive a
     * noticeable output signal.
     * 
     * @return
     */
    double minimalSignalDuration();
}
