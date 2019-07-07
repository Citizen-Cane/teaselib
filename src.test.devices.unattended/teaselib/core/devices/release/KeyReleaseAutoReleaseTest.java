package teaselib.core.devices.release;

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

public class KeyReleaseAutoReleaseTest {
    private static final Logger logger = LoggerFactory.getLogger(KeyReleaseAutoReleaseTest.class);

    KeyRelease keyRelease = KeyReleaseTest.connectDefaultDevice();
    Actuators actuators = KeyReleaseTest.connect(keyRelease);

    @Before
    public void releaseAllBefore() {
        keyRelease.actuators().stream().filter(Actuator::isRunning).forEach(Actuator::release);
    }

    @After
    public void releaseAllAfterwards() {
        keyRelease.actuators().stream().filter(Actuator::isRunning).forEach(Actuator::release);
    }

    @Test
    public void testThatArmWithoutStartReleasesKeyAfterRequestedDuration() throws InterruptedException {
        awaitAutoRelease(KeyReleaseTest::arm);
    }

    @Test
    public void testThatHoldWithoutStartReleasesKeyAfterRequestedDuration() throws InterruptedException {
        awaitAutoRelease(actuator -> {
            KeyReleaseTest.arm(actuator);
            KeyReleaseTest.sleep(10, TimeUnit.SECONDS);
            KeyReleaseTest.hold(actuator);
        });
    }

    private void awaitAutoRelease(Consumer<Actuator> test) throws InterruptedException {
        ExecutorService executor = NamedExecutorService.newFixedThreadPool(actuators.size(), "test", 0,
                TimeUnit.MINUTES);
        for (final Actuator actuator : KeyReleaseTest.connect(keyRelease)) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    test.accept(actuator);

                    // Intentionally don't start, this will
                    // block the device for the largest default duration of the actuators
                    // (60 minutes in default configuration)

                    // TODO Test doesn't detect restarted device because it just ends the test
                    // -> also control remaining duration
                    // - device will be unable to operate servos after about 2 hours due to low voltage,
                    // then reset without being able to move the servos anymore
                    // TODO Need to turn off servos when not needed
                    long previousRemaining = actuator.remaining(TimeUnit.SECONDS);
                    while (true) {
                        assertTrue(actuator.connected());
                        long remaining = actuator.remaining(TimeUnit.SECONDS);
                        assertTrue("Device rebooted", remaining <= previousRemaining);
                        if (remaining == 0) {
                            break;
                        }
                        logger.info("Actuator " + actuator + " has " + remaining / 60 + " minutes until release");
                        KeyReleaseTest.sleep(60, TimeUnit.SECONDS);
                    }

                    // Also Don't release, this should happen automatically

                    assertFalse(actuator.isRunning());
                    logger.info("Actuator " + actuator + " auto-released");
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        // When the first actuator is released, the color switches to "count
        // down" for the remaining actuators - should be green instead but
        // tolerable - the key release models "arm" by just starting to count
        // down from the default duration

        KeyReleaseTest.sleep(10, TimeUnit.SECONDS);
        assertTrue(keyRelease.connected());
        assertTrue(keyRelease.active());
        assertTrue(actuators.size() > 0);
    }

}
