/**
 * 
 */
package teaselib.core.devices.release;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.configuration.DebugSetup;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;

/**
 * @author Citizen-Cane
 *
 */
public class KeyReleaseTest {
    private static final Logger logger = LoggerFactory.getLogger(KeyReleaseTest.class);

    static final long HoldDuration = 1; // minutes

    public static KeyRelease connectDefaultDevice() {
        Devices devices = new Devices(DebugSetup.getConfigurationWithRemoteDeviceAccess());
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
        logger.info(keyRelease.getName() + ": " + actuators + " actuators");
        return actuators;
    }

    public static void arm(Actuator actuator) {
        assertFalse(actuator.isRunning());
        long available = actuator.available(TimeUnit.MINUTES);
        assertTrue(available > 0);
        actuator.arm();
        sleep(5, TimeUnit.SECONDS);
        assertTrue(actuator.isRunning());
    }

    public static void hold(Actuator actuator) {
        actuator.hold();
        assertTrue(actuator.isRunning());
        logger.info("Actuator " + actuator.index());
    }

    public static void start(Actuator actuator) {
        actuator.start(HoldDuration, TimeUnit.MINUTES);
        assertTrue(actuator.isRunning());
        logger.info("Actuator " + actuator.index());
    }

    public static void poll(Actuator actuator) {
        while (actuator.isRunning()) {
            long remaining = actuator.remaining(TimeUnit.SECONDS);
            if (remaining == 0) {
                break;
            }
            sleep(10, TimeUnit.SECONDS);
        }
    }

    public static void assertStoppedAfterRelease(Actuator actuator) {
        assertFalse(actuator.isRunning());
        sleep(10, TimeUnit.SECONDS);
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
        }
    }

    @Test
    public void testManualRelease() {
        KeyRelease keyRelease = connectDefaultDevice();

        for (Actuator actuator : connect(keyRelease)) {
            arm(actuator);
            start(actuator);
            poll(actuator);
            // Release the key in the last minute
            actuator.release();
            assertStoppedAfterRelease(actuator);
        }

        assertEndState(keyRelease);
    }

    @Test
    public void testAutomaticRelease() {
        KeyRelease keyRelease = connectDefaultDevice();

        for (Actuator actuator : connect(keyRelease)) {
            arm(actuator);
            start(actuator);
            poll(actuator);
            assertStoppedAfterRelease(actuator);
        }
        assertEndState(keyRelease);
    }

    @Test
    public void testDeepSleepRelease() {
        KeyRelease keyRelease = connectDefaultDevice();

        for (Actuator actuator : connect(keyRelease)) {
            arm(actuator);
            start(actuator);
            keyRelease.sleep(Integer.MAX_VALUE);
            // Sleeping longer than the next release duration with only one
            // release pending causes the device to enter deep sleep and
            // release on reset
            sleep(HoldDuration + 1, TimeUnit.MINUTES);
            // The key should have been released automatically by now
            assertStoppedAfterRelease(actuator);
        }

        assertEndState(keyRelease);
    }

    @Test
    public void testDeepSleepPacket() {
        KeyRelease keyRelease = connectDefaultDevice();

        Actuators actuators = connect(keyRelease);
        logger.info(keyRelease.getName() + ": " + actuators.size() + " actuators");
        Actuator actuator = actuators.get(0);
        actuator.sleep(HoldDuration, TimeUnit.MINUTES);

        // Trigger deep sleep by request a duration longer than the release duration
        // of the last running actuator. When the device wakes up from deep sleep,
        // it resets and subsequently releases the key.

        sleep(HoldDuration + 1, TimeUnit.MINUTES);
        assertEndState(keyRelease);
    }

    @Test
    public void testHardwiredDuration() {
        List<Long> durations_60_120 = Arrays.asList(60L, 120L);
        assertEquals(0, Actuators.getActuatorIndex(-59, durations_60_120));
        assertEquals(0, Actuators.getActuatorIndex(-60, durations_60_120));
        assertEquals(0, Actuators.getActuatorIndex(-61, durations_60_120));
        assertEquals(0, Actuators.getActuatorIndex(-90, durations_60_120));
        assertEquals(0, Actuators.getActuatorIndex(-119, durations_60_120));
        assertEquals(0, Actuators.getActuatorIndex(-120, durations_60_120));
        assertEquals(0, Actuators.getActuatorIndex(-121, durations_60_120));
        assertEquals(0, Actuators.getActuatorIndex(Long.MIN_VALUE, durations_60_120));

        assertEquals(0, Actuators.getActuatorIndex(-1, durations_60_120));
        assertEquals(0, Actuators.getActuatorIndex(0, durations_60_120));
        assertEquals(0, Actuators.getActuatorIndex(30, durations_60_120));
        assertEquals(0, Actuators.getActuatorIndex(59, durations_60_120));
        assertEquals(0, Actuators.getActuatorIndex(60, durations_60_120));

        assertEquals(1, Actuators.getActuatorIndex(61, durations_60_120));
        assertEquals(1, Actuators.getActuatorIndex(90, durations_60_120));
        assertEquals(1, Actuators.getActuatorIndex(120, durations_60_120));
        assertEquals(1, Actuators.getActuatorIndex(121, durations_60_120));
        assertEquals(1, Actuators.getActuatorIndex(Long.MAX_VALUE, durations_60_120));
    }
}
