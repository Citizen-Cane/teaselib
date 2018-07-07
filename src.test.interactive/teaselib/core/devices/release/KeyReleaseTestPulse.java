package teaselib.core.devices.release;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyReleaseTestPulse {
    private static final Logger logger = LoggerFactory.getLogger(KeyReleaseTestPulse.class);

    @Test
    public void testPulseFrequency() {
        final KeyRelease keyRelease = KeyReleaseTest.connectDefaultDevice();
        List<Actuator> actuators = KeyReleaseTest.connect(keyRelease);

        for (Actuator actuator : KeyReleaseTest.connect(keyRelease)) {
            testActuator(actuator);
        }

        KeyReleaseTest.sleep(10, TimeUnit.SECONDS);
        assertTrue(keyRelease.connected());
        assertTrue(keyRelease.active());
        assertTrue(!actuators.isEmpty());
    }

    private static void testActuator(Actuator actuator) {
        KeyReleaseTest.arm(actuator);
        KeyReleaseTest.sleep(5, TimeUnit.SECONDS);

        actuator.start(actuator.available(TimeUnit.SECONDS), TimeUnit.SECONDS);

        while (actuator.isRunning()) {
            long remaining = actuator.remaining(TimeUnit.SECONDS);
            if (remaining == 0) {
                break;
            }
            logger.info("Actuator {} has {} minutes until release", actuator, actuator.remaining(TimeUnit.MINUTES));
            KeyReleaseTest.sleep(60, TimeUnit.SECONDS);
        }

        // Don't release, this should happen automatically

        assertFalse(actuator.isRunning());
        logger.info("Actuator {} auto-released", actuator);
    }
}
