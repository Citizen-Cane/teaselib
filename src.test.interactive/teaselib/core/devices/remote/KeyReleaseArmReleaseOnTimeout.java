package teaselib.core.devices.remote;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.devices.DeviceCache;

public class KeyReleaseArmReleaseOnTimeout {
    private static final Logger logger = LoggerFactory.getLogger(KeyReleaseArmReleaseOnTimeout.class);

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(LocalNetworkDevice.EnableDeviceDiscovery, Boolean.TRUE.toString());
    }

    private static int connect(KeyRelease keyRelease) {
        Assume.assumeTrue("No KeyRelease device found", DeviceCache.connect(keyRelease, 10.0));
        assertTrue(keyRelease.connected());
        logger.info(keyRelease.getName());
        assertTrue(keyRelease.active());
        int actuators = keyRelease.actuators();
        assertTrue(actuators > 0);
        logger.info(keyRelease.getName() + ": " + actuators + " actuators");
        return actuators;
    }

    private static void arm(KeyRelease keyRelease, int actuator) {
        int available = keyRelease.available(actuator);
        assertTrue(available > 0);
        keyRelease.arm(actuator);
        sleepSeconds(1);
        assertTrue(keyRelease.isRunning(actuator));
    }

    private static void sleepSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            Assume.assumeTrue(false);
        }
    }

    @Test
    public void testThatArmWithoutStartReleasesKeyAfterRequestedDuration() throws InterruptedException {
        final KeyRelease keyRelease = KeyRelease.Devices.getDefaultDevice();
        int actuators = connect(keyRelease);

        ExecutorService executor = NamedExecutorService.newFixedThreadPool(actuators, "test", 0, TimeUnit.MINUTES);
        for (int i = 0; i < actuators; i++) {
            final int actuator = i;
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    testActuator(keyRelease, actuator);
                }
            });
        }

        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        // When the first actuator is released, the color switches to "count
        // down" for the remaining actuators - should be green instead but
        // tolerable - the key release models "arm" by just starting to count
        // down from the default duration

        sleepSeconds(10);
        assertTrue(keyRelease.connected());
        assertTrue(keyRelease.active());
        assertTrue(actuators > 0);
    }

    private static void testActuator(KeyRelease keyRelease, int actuator) {
        arm(keyRelease, actuator);

        // Intentionally don't start, this will
        // block the device for the largest default duration of the actuators
        // (60 minutes in default config)

        while (keyRelease.isRunning(actuator)) {
            int remaining = keyRelease.remaining(actuator);
            if (remaining == 0) {
                break;
            }
            logger.info("Actuator " + actuator + " has " + keyRelease.remaining(actuator) + " minutes until release");
            sleepSeconds(60);
        }

        // Also Don't release, this should happen automatically

        assertFalse(keyRelease.isRunning(actuator));
        logger.info("Actuator " + actuator + " has been released");
    }
}
