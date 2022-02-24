package teaselib.core.devices.release;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.devices.BatteryLevel;
import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.DeviceEvent;
import teaselib.core.devices.DeviceListener;

/**
 * @author Citizen-Cane
 *
 */
public class KeyReleaseBaseTest {
    private static final Logger logger = LoggerFactory.getLogger(KeyReleaseBaseTest.class);

    public static final long WAIT_FOR_CONNECTION_SECONDS = 20;
    public static final long HOLD_DURATION_MINUTES = 1;

    public static void releaseAllRunningActuators(KeyRelease keyRelease) {
        if (keyRelease != null) {
            keyRelease.actuators().stream().filter(Actuator::isRunning).forEach(Actuator::release);
        }
    }

    public static boolean connect(KeyRelease keyRelease, double secondsToWait) {
        return DeviceCache.connect(keyRelease, secondsToWait);
    }

    public static Actuators assertConnected(KeyRelease keyRelease) {
        return assertConnected(keyRelease, WAIT_FOR_CONNECTION_SECONDS);
    }

    public static Actuators assertConnected(KeyRelease keyRelease, long seconds) {
        assertTrue("No KeyRelease device found:" + keyRelease, DeviceCache.connect(keyRelease, seconds));
        assertTrue(keyRelease.connected());
        logger.info(keyRelease.getName());
        assertTrue(keyRelease.active());
        Actuators actuators = keyRelease.actuators();
        assertTrue(actuators.size() > 0);
        logger.info("{}: {} actuators", keyRelease.getName(), actuators);
        return actuators;
    }

    public static void arm(Actuator actuator) {
        assertFalse(actuator.isRunning());
        long available = actuator.available(TimeUnit.MINUTES);
        assertTrue(available > 0);
        actuator.arm();
        assertRunning(actuator);
    }

    public static void hold(Actuator actuator) {
        actuator.hold();
        assertRunning(actuator);
    }

    public static void start(Actuator actuator) {
        actuator.start(HOLD_DURATION_MINUTES, TimeUnit.MINUTES);
        assertRunning(actuator);
    }

    public static void waitForAutoRelease(Actuator actuator) {
        while (actuator.isRunning()) {
            long remaining = actuator.remaining(TimeUnit.SECONDS);
            if (remaining == 0) {
                break;
            }
            sleep(10, TimeUnit.SECONDS);
        }
    }

    private static void assertRunning(Actuator actuator) {
        assertTrue(actuator.isRunning());
        sleep(5, TimeUnit.SECONDS);
    }

    public static void assertStopped(Actuator actuator) {
        assertFalse(actuator.isRunning());
    }

    public static void assertEndState(KeyRelease keyRelease) {
        assertTrue(keyRelease.connected());
        assertTrue(keyRelease.active());
        assertTrue(keyRelease.actuators().size() > 0);
    }

    public static void sleep(long duration, TimeUnit unit) {
        try {
            Thread.sleep(TimeUnit.MILLISECONDS.convert(duration, unit));
        } catch (InterruptedException e) {
            Assume.assumeTrue(false);
            Thread.currentThread().interrupt();
        }
    }

    protected static class ActuatorMock implements Actuator {
        final long availableSeconds;
        long remainingSeconds;
        boolean isRunning;

        public ActuatorMock(long availableDuration, TimeUnit unit) {
            availableSeconds = TimeUnit.SECONDS.convert(availableDuration, unit);
            remainingSeconds = availableSeconds;
            isRunning = false;
        }

        @Override
        public String getDevicePath() {
            return DeviceCache.createDevicePath(getClass().getPackage().getName().replace('.', '/'),
                    "Actuator:" + Long.toString(availableSeconds));
        }

        @Override
        public String getName() {
            return getClass().getSimpleName();
        }

        @Override
        public boolean connected() {
            return false;
        }

        @Override
        public boolean active() {
            return false;
        }

        @Override
        public void close() {
            // Ignore
        }

        @Override
        public boolean isWireless() {
            return false;
        }

        @Override
        public BatteryLevel batteryLevel() {
            return null;
        }

        @Override
        public int index() {
            return 0;
        }

        @Override
        public boolean arm() {
            isRunning = true;
            return false;
        }

        @Override
        public void hold() { // Mock
            isRunning = true;
        }

        @Override
        public void start() { // Mock
            isRunning = true;
        }

        @Override
        public void start(long duration, TimeUnit unit) {
            long seconds = unit.toSeconds(duration);
            if (seconds > availableSeconds)
                throw new IllegalArgumentException(Long.toString(duration));
            remainingSeconds = seconds;
            isRunning = true;
        }

        @Override
        public int sleep(long duration, TimeUnit unit) {
            return 0;
        }

        @Override
        public boolean add(long duration, TimeUnit unit) {
            return false;
        }

        @Override
        public boolean isRunning() {
            return isRunning;
        }

        @Override
        public long available(TimeUnit unit) {
            return unit.convert(availableSeconds, TimeUnit.SECONDS);
        }

        @Override
        public long remaining(TimeUnit unit) {
            return unit.convert(remainingSeconds, TimeUnit.SECONDS);
        }

        @Override
        public boolean release() {
            isRunning = false;
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (availableSeconds ^ (availableSeconds >>> 32));
            result = prime * result + (int) (remainingSeconds ^ (remainingSeconds >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ActuatorMock other = (ActuatorMock) obj;
            if (availableSeconds != other.availableSeconds)
                return false;
            if (remainingSeconds != other.remainingSeconds)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return availableSeconds + " seconds";
        }

    }

    protected static class KeyReleaseMock extends KeyRelease {
        final Actuators actuators;

        public KeyReleaseMock(List<Actuator> actuators) {
            super(null, null);
            this.actuators = new Actuators(actuators);
        }

        @Override
        public Actuators actuators() {
            return actuators;
        }

    }

    protected static class DeviceEventMock implements DeviceEvent<KeyRelease> {
        final KeyRelease device;

        public DeviceEventMock(KeyRelease device) {
            super();
            this.device = device;
        }

        @Override
        public String getDevicePath() {
            return device.getDevicePath();
        }

        @Override
        public KeyRelease getDevice() {
            return device;
        }
    }

    public <T extends Device> void awaitConnection(DeviceCache<T> deviceCache) throws InterruptedException {
        CountDownLatch waitForConnection = new CountDownLatch(1);
        DeviceListener<T> deviceListener = new DeviceListener<>() {
            @Override
            public void deviceConnected(DeviceEvent<T> e) {
                waitForConnection.countDown();
            }

            @Override
            public void deviceDisconnected(DeviceEvent<T> e) {
                // Ignore
            }
        };
        try {
            deviceCache.addDeviceListener(deviceListener);
            if (!deviceCache.getDefaultDevice().connected()) {
                assertTrue("Didn't receive device connection event after" + WAIT_FOR_CONNECTION_SECONDS + " seconds",
                        waitForConnection.await(WAIT_FOR_CONNECTION_SECONDS, TimeUnit.SECONDS));
            }
        } finally {
            deviceCache.removeDeviceListener(deviceListener);
        }
    }

}
