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

    @Override
    public int index() {
        return index;
    }

    @Override
    public boolean arm() {
        return keyRelease.arm(index);
    }

    @Override
    public void hold() {
        keyRelease.hold(index);
    }

    @Override
    public void start() {
        start(0, TimeUnit.SECONDS);
    }

    @Override
    public void start(long duration, TimeUnit unit) {
        keyRelease.start(index, (int) TimeUnit.SECONDS.convert(duration, unit));
    }

    @Override
    public int sleep(long duration, TimeUnit unit) {
        return keyRelease.sleep((int) TimeUnit.SECONDS.convert(duration, unit));
    }

    @Override
    public boolean add(long duration, TimeUnit unit) {
        return keyRelease.add(index, (int) TimeUnit.SECONDS.convert(duration, unit));
    }

    /**
     * Whether the actuator is holding a key.
     * 
     * @return True if the actuator holds a key and is counting down (not armed).
     */
    @Override
    public boolean isRunning() {
        return keyRelease.isRunning(index);
    }

    /**
     * Get the number of minutes that can be added to this actuator.
     * 
     * @return The number of available minutes.
     */
    @Override
    public long available(TimeUnit unit) {
        return unit.convert(keyRelease.available(index), TimeUnit.SECONDS);
    }

    /**
     * @return Duration minutes until release.
     */
    @Override
    public long remaining(TimeUnit unit) {
        return unit.convert(keyRelease.remaining(index), TimeUnit.SECONDS);
    }

    /**
     * Release the key.
     * 
     * @return Whether the key has been released.
     */
    @Override
    public boolean release() {
        return keyRelease.release(index);
    }

    @Override
    public String toString() {
        return getName();
    }

}
