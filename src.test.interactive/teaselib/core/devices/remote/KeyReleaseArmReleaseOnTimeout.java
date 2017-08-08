package teaselib.core.devices.remote;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.devices.release.Actuator;
import teaselib.core.devices.release.KeyRelease;

public class KeyReleaseArmReleaseOnTimeout {
    private static final Logger logger = LoggerFactory.getLogger(KeyReleaseArmReleaseOnTimeout.class);

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(LocalNetworkDevice.EnableDeviceDiscovery, Boolean.TRUE.toString());
    }

    @Test
    public void testThatArmWithoutStartReleasesKeyAfterRequestedDuration() throws InterruptedException {
        final KeyRelease keyRelease = KeyRelease.Devices.getDefaultDevice();
        List<Actuator> actuators = KeyReleaseTest.connect(keyRelease);

        ExecutorService executor = NamedExecutorService.newFixedThreadPool(actuators.size(), "test", 0,
                TimeUnit.MINUTES);
        for (final Actuator actuator : KeyReleaseTest.connect(keyRelease)) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    testActuator(actuator);
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        // When the first actuator is released, the color switches to "count
        // down" for the remaining actuators - should be green instead but
        // tolerable - the key release models "arm" by just starting to count
        // down from the default duration

        KeyReleaseTest.sleepSeconds(10);
        assertTrue(keyRelease.connected());
        assertTrue(keyRelease.active());
        assertTrue(actuators.size() > 0);
    }

    private static void testActuator(Actuator actuator) {
        KeyReleaseTest.arm(actuator);

        // Intentionally don't start, this will
        // block the device for the largest default duration of the actuators
        // (60 minutes in default configuration)

        while (actuator.isRunning()) {
            int remaining = actuator.remaining();
            if (remaining == 0) {
                break;
            }
            logger.info("Actuator " + actuator + " has " + actuator.remaining() + " minutes until release");
            KeyReleaseTest.sleepSeconds(60);
        }

        // Also Don't release, this should happen automatically

        assertFalse(actuator.isRunning());
        logger.info("Actuator " + actuator + " auto-released");
    }
}
