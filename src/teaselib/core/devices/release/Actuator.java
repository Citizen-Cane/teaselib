package teaselib.core.devices.release;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import teaselib.core.StateImpl;
import teaselib.core.TeaseLib;
import teaselib.core.devices.BatteryLevel;
import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceCache;
import teaselib.core.util.Persist;

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

    public String start(long duration, TimeUnit unit) {
        return keyRelease.start(actuator, (int) TimeUnit.SECONDS.convert(duration, unit));
    }

    public int sleep(long duration, TimeUnit unit) {
        return keyRelease.sleep((int) TimeUnit.SECONDS.convert(duration, unit));
    }

    public boolean add(long duration, TimeUnit unit) {
        return keyRelease.add(actuator, (int) TimeUnit.SECONDS.convert(duration, unit));
    }

    /**
     * Whether the actuator is holding a key.
     * 
     * @return True if the actuator holds a key and is counting down (not armed).
     */
    public boolean isRunning() {
        return keyRelease.isRunning(actuator);
    }

    /**
     * Get the number of minutes that can be added to this actuator.
     * 
     * @return The number of available minutes.
     */
    public long available(TimeUnit unit) {
        return unit.convert(keyRelease.available(actuator), TimeUnit.SECONDS);
    }

    /**
     * @return Duration minutes until release.
     */
    public long remaining(TimeUnit unit) {
        return unit.convert(keyRelease.remaining(actuator), TimeUnit.SECONDS);
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

    public static final class RemoveActionItem extends StateImpl implements Persist.Persistable {
        private final KeyRelease keyRelease;
        private final int actuator;

        public RemoveActionItem(TeaseLib teaseLib, String domain, String devicePath) {
            super(teaseLib, TeaseLib.DefaultDomain, devicePath);
            this.keyRelease = KeyRelease.getDeviceCache(teaseLib.devices, teaseLib.config).getDevice(devicePath);
            // TODO improve getting the actuator device
            this.actuator = getActuatorIndex(devicePath);
        }

        private int getActuatorIndex(String devicePath) {
            return Integer.parseInt(devicePath.substring(devicePath.lastIndexOf("/")));
        }

        @Override
        public List<String> persisted() {
            return Arrays.asList(Persist.persist(domain), Persist.persist(item.toString()));
        }

        public RemoveActionItem(Persist.Storage storage) {
            this(storage.getInstance(TeaseLib.class), storage.next(), storage.next());
        }

        public String devicePath() {
            return item.toString();
        }

        @Override
        public Persistence remove() {
            DeviceCache.connect(keyRelease, 10.0);
            keyRelease.actuators().get(actuator).release();
            return this;
        }
    }

    // TODO Could be the actual item, since it doesn't reveal internal state -> support via TeaseLib
    public String releaseItem(TeaseLib teaseLib) {
        return Persist.persist(new RemoveActionItem(teaseLib, TeaseLib.DefaultDomain, getDevicePath()));
    }
}
