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
     * 
     * @param value
     *            The stimulation value from 0.0-1.0
     */
    void set(double value);

    String getDeviceName();

    String getLocation();

    ChannelDependency channelDependency();

    Object getDevice();
}
