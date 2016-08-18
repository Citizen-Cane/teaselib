/**
 * 
 */
package teaselib.core.devices.remote;

import static org.junit.Assert.*;

import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.devices.DeviceCache;

/**
 * @author someone
 *
 */
public class KeyReleaseTest {
    private static final Logger logger = LoggerFactory
            .getLogger(KeyReleaseTest.class);

    static final int Minutes = 2;

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
        int actuators = keyRelease.actuators();
        logger.info(keyRelease.getName() + ": " + actuators + " actuators");
        Assume.assumeTrue(DeviceCache.connect(keyRelease, 5.0));
        assertTrue(keyRelease.connected());
        assertTrue(actuators > 0);
        for (int i = 0; i < actuators; i++) {
            int available = keyRelease.available(i);
            assertTrue(available > 0);
            keyRelease.arm(i);
            sleepSeconds(10);
            keyRelease.start(i, Minutes);
            assertTrue(keyRelease.isRunning(i));
            logger.info("Actuator " + i);
            while (keyRelease.isRunning(i)) {
                int remaining = keyRelease.remaining(i);
                if (remaining == 0) {
                    break;
                }
                sleepSeconds(10);
            }
            // Release the key in the last minute
            keyRelease.release(i);
            assertFalse(keyRelease.isRunning(i));
            sleepSeconds(10);
        }
    }

    @Test
    public void testAutomaticRelease() {
        KeyRelease keyRelease = KeyRelease.Devices.getDefaultDevice();
        int actuators = keyRelease.actuators();
        logger.info(keyRelease.getName() + ": " + actuators + " actuators");
        Assume.assumeTrue(DeviceCache.connect(keyRelease, 5.0));
        assertTrue(keyRelease.connected());
        assertTrue(actuators > 0);
        for (int i = 0; i < actuators; i++) {
            int available = keyRelease.available(i);
            assertTrue(available > 0);
            keyRelease.arm(i);
            sleepSeconds(10);
            keyRelease.start(i, Minutes);
            assertTrue(keyRelease.isRunning(i));
            logger.info("Actuator " + i);
            while (keyRelease.isRunning(i)) {
                int remaining = keyRelease.remaining(i);
                if (remaining == 0) {
                    break;
                }
                sleepSeconds(10);
            }
            // The key should have been released automatically by now
            assertFalse(keyRelease.isRunning(i));
            sleepSeconds(10);
        }
    }

    @Test
    public void testDeepSleepRelease() {
        KeyRelease keyRelease = KeyRelease.Devices.getDefaultDevice();
        int actuators = keyRelease.actuators();
        logger.info(keyRelease.getName() + ": " + actuators + " actuators");
        Assume.assumeTrue(DeviceCache.connect(keyRelease, 5.0));
        assertTrue(keyRelease.connected());
        assertTrue(actuators > 0);
        for (int i = 0; i < actuators; i++) {
            int available = keyRelease.available(i);
            assertTrue(available > 0);
            keyRelease.arm(i);
            sleepSeconds(10);
            keyRelease.start(i, Minutes);
            assertTrue(keyRelease.isRunning(i));
            logger.info("Actuator " + i);
            keyRelease.sleep(Integer.MAX_VALUE);
            // Sleeping longer than the next release duration with only one
            // release pending causes the device to enter deep sleep and
            // release on reset
            sleepMinutes(Minutes);
            // The key should have been released automatically by now
            assertFalse(keyRelease.isRunning(i));
            sleepSeconds(10);
        }
    }

    @Test
    public void testDeepSleepPacket() {
        KeyRelease keyRelease = KeyRelease.Devices.getDefaultDevice();
        logger.info(keyRelease.getName());
        Assume.assumeTrue(DeviceCache.connect(keyRelease, 5.0));
        assertTrue(keyRelease.connected());
        keyRelease.sleep(Minutes);
        // Sleeping longer than the next release duration with only one
        // release pending causes the device to enter deep sleep and
        // release on reset
        sleepMinutes(Minutes);
        assertTrue(keyRelease.connected());
    }
}
