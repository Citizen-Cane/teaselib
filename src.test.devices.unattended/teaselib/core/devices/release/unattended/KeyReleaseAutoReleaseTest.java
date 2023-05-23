package teaselib.core.devices.release.unattended;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.devices.Devices;
import teaselib.core.devices.release.Actuator;
import teaselib.core.devices.release.Actuators;
import teaselib.core.devices.release.KeyRelease;
import teaselib.core.devices.release.KeyReleaseBaseTest;

public class KeyReleaseAutoReleaseTest extends KeyReleaseBaseTest {

    private static final Logger logger = LoggerFactory.getLogger(KeyReleaseAutoReleaseTest.class);

    static final long requestedDurationSeconds = HOURS.toSeconds(1);

    final Devices devices = new Devices(DebugSetup.getConfigurationWithRemoteDeviceAccess());
    final KeyRelease keyRelease = devices.getDefaultDevice(KeyRelease.class);

    @Before
    public void before() {
        assertConnected(keyRelease);
        releaseAllRunningActuators(keyRelease);

        Actuators actuators = keyRelease.actuators();
        assertFalse("No actuators found", actuators.available().isEmpty());
        assertTrue("All actuators still active", actuators.available().size() > 0);
        assertEquals("Actuators still active", actuators.available().size(), actuators.size());
    }

    @After
    public void after() {
        releaseAllRunningActuators(keyRelease);
        devices.close();
    }

    @Test
    public void testThatArmWithoutStartReleasesKeyAfterRequestedDuration() throws InterruptedException {
        awaitAutoRelease(KeyReleaseBaseTest::arm);
    }

    @Test
    public void testThatHoldWithoutStartReleasesKeyAfterRequestedDuration() throws InterruptedException {
        awaitAutoRelease(actuator -> {
            arm(actuator);
            sleep(10, TimeUnit.SECONDS);
            hold(actuator);
        });
    }

    private void awaitAutoRelease(Consumer<Actuator> test) throws InterruptedException {
        ExecutorService executor = NamedExecutorService.newFixedThreadPool(keyRelease.actuators().size(), "test", 0,
                TimeUnit.MINUTES);
        for (final Actuator actuator : assertConnected(keyRelease)) {
            executor.submit(() -> {
                test.accept(actuator);

                // Intentionally don't start, this will
                // block the device for the largest default duration of the actuators
                // (60 minutes in default configuration)

                // TODO Test doesn't detect restarted device because it just ends the test
                // -> also control remaining duration
                // - when running on battery,
                // device will be unable to operate servos after about 2 hours due to low voltage,
                // then reset without being able to move the servos anymore
                // TODO turn off servos when not needed
                long previousRemaining = actuator.remaining(TimeUnit.SECONDS);
                while (true) {
                    assertTrue(actuator.connected());
                    long remaining = actuator.remaining(TimeUnit.SECONDS);
                    assertTrue("Device rebooted", remaining <= previousRemaining);
                    if (remaining == 0) {
                        break;
                    }
                    logger.info("Actuator {} has {} minutes until release", actuator, remaining / 60);
                    sleep(60, TimeUnit.SECONDS);
                }

                // Also Don't release, this should happen automatically

                assertFalse(actuator.isRunning());
                logger.info("Actuator {} auto-released", actuator);
            });
        }

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        // When the first actuator is released, the color switches to "count
        // down" for the remaining actuators - should be green instead but
        // tolerable - the key release models "arm" by just starting to count
        // down from the default duration

        sleep(10, TimeUnit.SECONDS);
        assertTrue(keyRelease.connected());
        assertTrue(keyRelease.active());
        assertTrue(keyRelease.actuators().size() > 0);
    }

}
