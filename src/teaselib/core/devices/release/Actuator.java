package teaselib.core.devices.release;

import teaselib.core.devices.BatteryLevel;
import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceCache;

/**
 * @author Citizen-Cane
 *
 */
public class Actuator implements Device {
    final KeyRelease keyRelease;
    final int actuator;

    public Actuator(KeyRelease keyRelease, int actuatorIndex) {
        this.keyRelease = keyRelease;
        this.actuator = actuatorIndex;
    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(keyRelease.getDevicePath(), Integer.toString(actuator));
    }

    @Override
    public String getName() {
        return keyRelease.getName() + " actuator " + actuator;
    }

    @Override
    public boolean connected() {
        return keyRelease.connected();
    }

    @Override
    public boolean active() {
        return keyRelease.active();
    }

    @Override
    public void close() {
        keyRelease.close();
    }

    @Override
    public boolean isWireless() {
        return keyRelease.isWireless();
    }

    @Override
    public BatteryLevel batteryLevel() {
        return keyRelease.batteryLevel();
    }

    public int index() {
        return actuator;
    }

    public boolean arm() {
        return keyRelease.arm(actuator);
    }

    public String start(int timeMinutes) {
        return keyRelease.start(actuator, timeMinutes);
    }

    public int sleep(int timeMinutes) {
        return keyRelease.sleep(timeMinutes);
    }

    public boolean add(int minutes) {
        return keyRelease.add(actuator, minutes);
    }

    /**
     * Whether the actuator is holding a key.
     * 
     * @return True if the actuator holds a key and is counting down (not
     *         armed).
     */
    public boolean isRunning() {
        return keyRelease.isRunning(actuator);
    }

    /**
     * Get the number of minutes that can be added to this actuator.
     * 
     * @return The number of available minutes.
     */
    public int available() {
        return keyRelease.available(actuator);
    }

    /**
     * @return Duration minutes until release.
     */
    public int remaining() {
        return keyRelease.remaining(actuator);
    }

    /**
     * Release the key.
     * 
     * @return Whether the key has been released.
     */
    public boolean release() {
        return keyRelease.release(actuator);
    }

    @Override
    public String toString() {
        return getName();
    }

}
