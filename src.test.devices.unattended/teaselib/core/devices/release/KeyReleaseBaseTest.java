package teaselib.core.devices.release;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Assume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.configuration.DebugSetup;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;

/**
 * @author Citizen-Cane
 *
 */
public class KeyReleaseBaseTest {
    private static final Logger logger = LoggerFactory.getLogger(KeyReleaseBaseTest.class);

    static final long HOLD_DURATION_MINUTES = 1;

    public static void releaseAllRunningActuators(KeyRelease keyRelease) {
        if (keyRelease != null) {
            keyRelease.actuators().stream().filter(Actuator::isRunning).forEach(Actuator::release);
        }
    }

    public static KeyRelease connectDefaultDevice() {
        Devices devices = new Devices(DebugSetup.getConfigurationWithRemoteDeviceAccess());
        return connectDefaultDevice(devices);
    }

    public static KeyRelease connectDefaultDevice(Devices devices) {
        DeviceCache<KeyRelease> deviceCache = devices.get(KeyRelease.class);
        KeyRelease keyRelease = deviceCache.getDefaultDevice();
        connect(keyRelease);
        return keyRelease;
    }

    public static Actuators connect(KeyRelease keyRelease) {
        assertTrue("No KeyRelease device found", DeviceCache.connect(keyRelease, 0.0));
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

    static void sleep(long duration, TimeUnit unit) {
        try {
            Thread.sleep(TimeUnit.MILLISECONDS.convert(duration, unit));
        } catch (InterruptedException e) {
            Assume.assumeTrue(false);
            Thread.currentThread().interrupt();
        }
    }

}
