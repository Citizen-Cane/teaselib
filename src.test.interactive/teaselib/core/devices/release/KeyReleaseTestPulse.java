package teaselib.core.devices.release;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyReleaseTestPulse {
    private static final Logger logger = LoggerFactory.getLogger(KeyReleaseTestPulse.class);

    @Test
    public void testPulseFrequency() {
        final KeyRelease keyRelease = KeyReleaseFunctionalTest.connectDefaultDevice();
        Actuators actuators = KeyReleaseFunctionalTest.connect(keyRelease);

        for (Actuator actuator : KeyReleaseFunctionalTest.connect(keyRelease)) {
            testActuator(actuator);
        }

        KeyReleaseFunctionalTest.sleep(10, TimeUnit.SECONDS);
        assertTrue(keyRelease.connected());
        assertTrue(keyRelease.active());
        assertTrue(!actuators.isEmpty());
    }

    private static void testActuator(Actuator actuator) {
        KeyReleaseFunctionalTest.arm(actuator);
        KeyReleaseFunctionalTest.sleep(5, TimeUnit.SECONDS);

        actuator.start(actuator.available(TimeUnit.SECONDS), TimeUnit.SECONDS);

        while (actuator.isRunning()) {
            long remaining = actuator.remaining(TimeUnit.SECONDS);
            if (remaining == 0) {
                break;
            }
            logger.info("Actuator {} has {} minutes until release", actuator, actuator.remaining(TimeUnit.MINUTES));
            KeyReleaseFunctionalTest.sleep(60, TimeUnit.SECONDS);
        }

        // Don't release, this should happen automatically

        assertFalse(actuator.isRunning());
        logger.info("Actuator {} auto-released", actuator);
    }
}
