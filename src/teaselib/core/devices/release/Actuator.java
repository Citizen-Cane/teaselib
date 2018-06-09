package teaselib.core.devices.release;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import teaselib.State;
import teaselib.core.TeaseLib;
import teaselib.core.devices.BatteryLevel;
import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.ReleaseAction;
import teaselib.core.util.Persist;

// TODO Should be a device -> just remember the device path instead of the additional index

/**
 * @author Citizen-Cane
 *
 */
public class Actuator implements Device {
    final KeyRelease keyRelease;
    final int index;

    public Actuator(KeyRelease keyRelease, int actuatorIndex) {
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

    public String start(long duration, TimeUnit unit) {
        return keyRelease.start(index, (int) TimeUnit.SECONDS.convert(duration, unit));
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

    // TODO Should be a private class but need to instanciate it when persistence is required
    // - find out if constructor.setAccesible() does the trick
    public static final class ActuatorReleaseAction extends ReleaseAction {
        private final KeyRelease keyRelease;
        private final int actuator;

        public ActuatorReleaseAction(TeaseLib teaseLib, String domain, String devicePath) {
            super(teaseLib, domain, devicePath);
            String parentDevice = DeviceCache.getParentDevice(devicePath);
            this.keyRelease = KeyRelease.getDeviceCache(teaseLib.devices, teaseLib.config).getDevice(parentDevice);
            this.actuator = getActuatorIndex(devicePath);
        }

        private static int getActuatorIndex(String devicePath) {
            String actuatorIndex = devicePath.substring(devicePath.lastIndexOf('/') + 1);
            return Integer.parseInt(actuatorIndex);
        }

        @Override
        public List<String> persisted() {
            return Arrays.asList(Persist.persist(domain), Persist.persist(item.toString()));
        }

        public ActuatorReleaseAction(Persist.Storage storage) {
            this(storage.getInstance(TeaseLib.class), storage.next(), storage.next());
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        @Override
        public Persistence remove() {
            DeviceCache.connect(keyRelease, 10.0);
            keyRelease.actuators().get(actuator).release();
            return this;
        }
    }

    public State releaseAction(TeaseLib teaseLib) {
        return new ActuatorReleaseAction(teaseLib, TeaseLib.DefaultDomain, getDevicePath());
    }
}
