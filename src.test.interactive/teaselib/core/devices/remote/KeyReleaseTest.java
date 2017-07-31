/**
 * 
 */
package teaselib.core.devices.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.devices.DeviceCache;

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

    private static int connect(KeyRelease keyRelease) {
        Assume.assumeTrue(DeviceCache.connect(keyRelease, 10.0));
        assertTrue(keyRelease.connected());
        logger.info(keyRelease.getName());
        assertTrue(keyRelease.active());
        int actuators = keyRelease.actuators();
        assertTrue(actuators > 0);
        logger.info(keyRelease.getName() + ": " + actuators + " actuators");
        return actuators;
    }

    private static void arm(KeyRelease keyRelease, int i) {
        int available = keyRelease.available(i);
        assertTrue(available > 0);
        keyRelease.arm(i);
        sleepSeconds(1);
        assertTrue(keyRelease.isRunning(i));
    }

    private static void start(KeyRelease keyRelease, int i) {
        keyRelease.start(i, Minutes);
        assertTrue(keyRelease.isRunning(i));
        logger.info("Actuator " + i);
    }

    private static void poll(KeyRelease keyRelease, int i) {
        while (keyRelease.isRunning(i)) {
            int remaining = keyRelease.remaining(i);
            if (remaining == 0) {
                break;
            }
            sleepSeconds(10);
        }
    }

    private static void assertRunningAfterRelease(KeyRelease keyRelease, int i) {
        assertFalse(keyRelease.isRunning(i));
        sleepSeconds(10);
    }

    private static void assertEndState(KeyRelease keyRelease, int actuators) {
        assertTrue(keyRelease.connected());
        assertTrue(keyRelease.active());
        assertTrue(actuators > 0);
    }

    private static void sleepSeconds(int seconds) {
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
        int actuators = connect(keyRelease);
        for (int i = 0; i < actuators; i++) {
            arm(keyRelease, i);
            start(keyRelease, i);
            poll(keyRelease, i);
            // Release the key in the last minute
            keyRelease.release(i);
            assertRunningAfterRelease(keyRelease, i);
        }
        assertEndState(keyRelease, actuators);
    }

    @Test
    public void testAutomaticRelease() {
        KeyRelease keyRelease = KeyRelease.Devices.getDefaultDevice();
        int actuators = connect(keyRelease);
        for (int i = 0; i < actuators; i++) {
            arm(keyRelease, i);
            start(keyRelease, i);
            poll(keyRelease, i);
            assertRunningAfterRelease(keyRelease, i);
        }
        assertEndState(keyRelease, actuators);
    }

    @Test
    public void testDeepSleepRelease() {
        KeyRelease keyRelease = KeyRelease.Devices.getDefaultDevice();
        int actuators = connect(keyRelease);
        for (int i = 0; i < actuators; i++) {
            arm(keyRelease, i);
            start(keyRelease, i);
            keyRelease.sleep(Integer.MAX_VALUE);
            // Sleeping longer than the next release duration with only one
            // release pending causes the device to enter deep sleep and
            // release on reset
            sleepMinutes(Minutes + 1);
            // The key should have been released automatically by now
            assertRunningAfterRelease(keyRelease, i);
        }
        assertEndState(keyRelease, actuators);
    }

    @Test
    public void testDeepSleepPacket() {
        KeyRelease keyRelease = KeyRelease.Devices.getDefaultDevice();
        int actuators = connect(keyRelease);
        logger.info(keyRelease.getName() + ": " + actuators + " actuators");
        keyRelease.sleep(Minutes);
        // Sleeping longer than the next release duration with only one
        // release pending causes the device to enter deep sleep and
        // release on reset
        sleepMinutes(Minutes + 1);
        assertEndState(keyRelease, actuators);
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
