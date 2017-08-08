/**
 * 
 */
package teaselib.core.devices.remote;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.release.Actuator;
import teaselib.core.devices.release.KeyRelease;

/**
 * @author Citizen-Cane
 *
 */
public class KeyReleaseTest {
    private static final Logger logger = LoggerFactory.getLogger(KeyReleaseTest.class);

    static final int Minutes = 2;

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(LocalNetworkDevice.EnableDeviceDiscovery, Boolean.TRUE.toString());
    }

    public static List<Actuator> connect(KeyRelease keyRelease) {
        Assume.assumeTrue(DeviceCache.connect(keyRelease, 10.0));
        assertTrue(keyRelease.connected());
        logger.info(keyRelease.getName());
        assertTrue(keyRelease.active());
        List<Actuator> actuators = keyRelease.actuators();
        assertTrue(actuators.size() > 0);
        logger.info(keyRelease.getName() + ": " + actuators + " actuators");
        return actuators;
    }

    public static void arm(Actuator actuator) {
        int available = actuator.available();
        assertTrue(available > 0);
        actuator.arm();
        sleepSeconds(1);
        assertTrue(actuator.isRunning());
    }

    public static void start(Actuator actuator) {
        actuator.start(Minutes);
        assertTrue(actuator.isRunning());
        logger.info("Actuator " + actuator.index());
    }

    public static void poll(Actuator actuator) {
        while (actuator.isRunning()) {
            int remaining = actuator.remaining();
            if (remaining == 0) {
                break;
            }
            sleepSeconds(10);
        }
    }

    public static void assertStoppedAfterRelease(Actuator actuator) {
        assertFalse(actuator.isRunning());
        sleepSeconds(10);
    }

    public static void assertEndState(KeyRelease keyRelease) {
        assertTrue(keyRelease.connected());
        assertTrue(keyRelease.active());
        assertTrue(keyRelease.actuators().size() > 0);
    }

    public static void sleepSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            Assume.assumeTrue(false);
        }
    }

    private static void sleepMinutes(int minutes) {
        try {
            Thread.sleep(minutes * 1000 * 60);
        } catch (InterruptedException e) {
            Assume.assumeTrue(false);
        }
    }

    @Test
    public void testManualRelease() {
        KeyRelease keyRelease = KeyRelease.Devices.getDefaultDevice();
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
        KeyRelease keyRelease = KeyRelease.Devices.getDefaultDevice();
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
        KeyRelease keyRelease = KeyRelease.Devices.getDefaultDevice();
        for (Actuator actuator : connect(keyRelease)) {
            arm(actuator);
            start(actuator);
            keyRelease.sleep(Integer.MAX_VALUE);
            // Sleeping longer than the next release duration with only one
            // release pending causes the device to enter deep sleep and
            // release on reset
            sleepMinutes(Minutes + 1);
            // The key should have been released automatically by now
            assertStoppedAfterRelease(actuator);
        }
        assertEndState(keyRelease);
    }

    @Test
    public void testDeepSleepPacket() {
        KeyRelease keyRelease = KeyRelease.Devices.getDefaultDevice();
        List<Actuator> actuators = connect(keyRelease);
        logger.info(keyRelease.getName() + ": " + actuators.size() + " actuators");
        keyRelease.sleep(Minutes);
        // Sleeping longer than the next release duration with only one
        // release pending causes the device to enter deep sleep and
        // release on reset
        sleepMinutes(Minutes + 1);
        assertEndState(keyRelease);
    }

    @Test
    public void testHardwiredDuration() {
        assertEquals(0, KeyRelease.getBestActuator(59, Arrays.asList(60, 120)));
        assertEquals(0, KeyRelease.getBestActuator(60, Arrays.asList(60, 120)));
        assertEquals(1, KeyRelease.getBestActuator(61, Arrays.asList(60, 120)));
        assertEquals(1, KeyRelease.getBestActuator(120, Arrays.asList(60, 120)));
        assertEquals(1, KeyRelease.getBestActuator(121, Arrays.asList(60, 120)));
        assertEquals(0, KeyRelease.getBestActuator(0, Arrays.asList(60, 120)));
        assertEquals(0, KeyRelease.getBestActuator(-1, Arrays.asList(60, 120)));
    }

}
