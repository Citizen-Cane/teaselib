package teaselib.core.devices.release;

import java.util.concurrent.TimeUnit;

import teaselib.core.devices.BatteryLevel;
import teaselib.core.devices.DeviceCache;

/**
 * @author Citizen-Cane
 *
 */
public class KeyReleaseActuator implements Actuator {
    final KeyRelease keyRelease;
    final int index;

    public KeyReleaseActuator(KeyRelease keyRelease, int actuatorIndex) {
        this.keyRelease = keyRelease;
        this.index = actuatorIndex;
    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(keyRelease.getDevicePath(), Integer.toString(index));
    }

    @Override
    public String getName() {
        return keyRelease.getName() + " actuator " + index;
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
        return index;
    }

    public boolean arm() {
        return keyRelease.arm(index);
    }

    public void hold() {
        keyRelease.hold(index, (int) TimeUnit.SECONDS.convert(keyRelease.available(index), TimeUnit.SECONDS));
    }

    public void start() {
        // TODO Reset on device
        hold();
        start(keyRelease.available(index), TimeUnit.SECONDS);
    }

    public void start(long duration, TimeUnit unit) {
        // TODO Reset on device
        hold();
        keyRelease.start(index, (int) TimeUnit.SECONDS.convert(duration, unit));
    }

    public int sleep(long duration, TimeUnit unit) {
        return keyRelease.sleep((int) TimeUnit.SECONDS.convert(duration, unit));
    }

    public boolean add(long duration, TimeUnit unit) {
        return keyRelease.add(index, (int) TimeUnit.SECONDS.convert(duration, unit));
    }

    /**
     * Whether the actuator is holding a key.
     * 
     * @return True if the actuator holds a key and is counting down (not armed).
     */
    public boolean isRunning() {
        return keyRelease.isRunning(index);
    }

    /**
     * Get the number of minutes that can be added to this actuator.
     * 
     * @return The number of available minutes.
     */
    public long available(TimeUnit unit) {
        return unit.convert(keyRelease.available(index), TimeUnit.SECONDS);
    }

    /**
     * @return Duration minutes until release.
     */
    public long remaining(TimeUnit unit) {
        return unit.convert(keyRelease.remaining(index), TimeUnit.SECONDS);
    }

    /**
     * Release the key.
     * 
     * @return Whether the key has been released.
     */
    public boolean release() {
        return keyRelease.release(index);
    }

    @Override
    public String toString() {
        return getName();
    }

}
