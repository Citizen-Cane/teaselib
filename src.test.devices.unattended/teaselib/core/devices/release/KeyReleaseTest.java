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

    static final long HOLD_DURATION_MINUTES = 1;

    public static void releaseAllRunningActuators(KeyRelease keyRelease) {
        keyRelease.actuators().stream().filter(Actuator::isRunning).forEach(Actuator::release);
    }

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

    @Test
    public void testManualRelease() {
        KeyRelease keyRelease = connectDefaultDevice();

        for (Actuator actuator : connect(keyRelease)) {
            arm(actuator);
            start(actuator);
            waitForAutoRelease(actuator);
            // Release the key in the last minute
            actuator.release();
            assertStopped(actuator);
        }

        assertEndState(keyRelease);
    }

    @Test
    public void testAutomaticRelease() {
        KeyRelease keyRelease = connectDefaultDevice();

        for (Actuator actuator : connect(keyRelease)) {
            arm(actuator);
            start(actuator);
            waitForAutoRelease(actuator);
            assertStopped(actuator);
        }
        assertEndState(keyRelease);
    }

    @Test(expected = IllegalStateException.class)
    public void testWrongCall() {
        KeyRelease keyRelease = connectDefaultDevice();

        for (Actuator actuator : connect(keyRelease)) {
            arm(actuator);
            start(actuator);
            hold(actuator);
            waitForAutoRelease(actuator);
            assertStopped(actuator);
        }
        assertEndState(keyRelease);
    }

    @Test
    public void testWrongCallReleasesAll() {
        KeyRelease keyRelease = connectDefaultDevice();

        for (Actuator actuator : connect(keyRelease)) {
            arm(actuator);
            hold(actuator);
            start(actuator);
        }

        Actuator wrongCalled = keyRelease.actuators().get(0);
        try {
            hold(wrongCalled);
            fail("Expected WrongCallException");
        } catch (IllegalStateException e) {
            for (Actuator actuator : connect(keyRelease)) {
                assertStopped(actuator);
            }
        } catch (Exception e) {
            fail("Expected WrongCallException");
        }

        assertEndState(keyRelease);
    }

    @Test
    public void testStatus() {
        KeyRelease keyRelease = connectDefaultDevice();

        for (Actuator actuator : connect(keyRelease)) {
            arm(actuator);
            start(actuator);
            waitForAutoRelease(actuator);
            assertStopped(actuator);
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
            sleep(HOLD_DURATION_MINUTES + 1, TimeUnit.MINUTES);
            // The key should have been released automatically by now
            assertStopped(actuator);
        }

        assertEndState(keyRelease);
    }

    @Test
    public void testDeepSleepPacket() {
        KeyRelease keyRelease = connectDefaultDevice();
        Actuators actuators = connect(keyRelease);

        Actuator actuator = actuators.get(0);
        actuator.sleep(HOLD_DURATION_MINUTES, TimeUnit.MINUTES);

        // Trigger deep sleep by request a duration longer than the release duration
        // of the last running actuator. When the device wakes up from deep sleep,
        // it resets and subsequently releases the key.
        sleep(HOLD_DURATION_MINUTES + 1, TimeUnit.MINUTES);
        assertEndState(keyRelease);
    }

    @Test
    public void testHardwiredDuration() {
        List<Long> durations = Arrays.asList(60L, 120L);
        assertEquals(0, Actuators.getActuatorIndex(-59, durations));
        assertEquals(0, Actuators.getActuatorIndex(-60, durations));
        assertEquals(0, Actuators.getActuatorIndex(-61, durations));
        assertEquals(0, Actuators.getActuatorIndex(-90, durations));
        assertEquals(0, Actuators.getActuatorIndex(-119, durations));
        assertEquals(0, Actuators.getActuatorIndex(-120, durations));
        assertEquals(0, Actuators.getActuatorIndex(-121, durations));
        assertEquals(0, Actuators.getActuatorIndex(Long.MIN_VALUE, durations));

        assertEquals(0, Actuators.getActuatorIndex(-1, durations));
        assertEquals(0, Actuators.getActuatorIndex(0, durations));
        assertEquals(0, Actuators.getActuatorIndex(30, durations));
        assertEquals(0, Actuators.getActuatorIndex(59, durations));
        assertEquals(0, Actuators.getActuatorIndex(60, durations));

        assertEquals(1, Actuators.getActuatorIndex(61, durations));
        assertEquals(1, Actuators.getActuatorIndex(90, durations));
        assertEquals(1, Actuators.getActuatorIndex(120, durations));
        assertEquals(1, Actuators.getActuatorIndex(121, durations));
        assertEquals(1, Actuators.getActuatorIndex(Long.MAX_VALUE, durations));
    }
}
