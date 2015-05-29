/**
 * 
 */
package teaselib.stimulation;

/**
 * @author someone
 *
 */
public interface Stimulator {
    /**
     * Control a stimulator. A device may consists of multiple stimulators (like
     * gamepads), in that case two stimulator instances are created for the
     * device.
     * 
     * @param value
     *            The stimulation value from 0.0-1.0
     */
    void set(double value);

    String getDeviceName();
}
