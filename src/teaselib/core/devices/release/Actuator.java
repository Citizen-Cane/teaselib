package teaselib.core.devices.release;

import java.util.concurrent.TimeUnit;

import teaselib.core.devices.Device;

// TODO Should be a device -> just remember the device path instead of the additional index

/**
 * @author Citizen-Cane
 *
 */
public interface Actuator extends Device {

    public int index();

    public boolean arm();

    public void hold();

    public void start();

    public void start(long duration, TimeUnit unit);

    public int sleep(long duration, TimeUnit unit);

    public boolean add(long duration, TimeUnit unit);

    /**
     * Whether the actuator is holding a key.
     * 
     * @return True if the actuator holds a key and is counting down (not armed).
     */
    public boolean isRunning();

    /**
     * Get the number of minutes that can be added to this actuator.
     * 
     * @return The number of available minutes.
     */
    public long available(TimeUnit unit);

    /**
     * @return Duration minutes until release.
     */
    public long remaining(TimeUnit unit);

    /**
     * Release the key.
     * 
     * @return Whether the key has been released.
     */
    public boolean release();
}
