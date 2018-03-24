package teaselib.motiondetection;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Configuration;
import teaselib.core.devices.Devices;
import teaselib.motiondetection.MotionDetector.MotionSensitivity;
import teaselib.motiondetection.MotionDetector.Presence;
import teaselib.test.DebugSetup;

/**
 * @author Citizen-Cane
 *
 */
public class TestMotionDetectorStability {
    private static final Logger logger = LoggerFactory.getLogger(TestMotionDetectorStability.class);

    @Test
    public void testPauseResume() {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);

        MotionDetector md = devices.get(MotionDetector.class).getDefaultDevice();
        md.setSensitivity(MotionSensitivity.Normal);

        int durationMillis = 10 * 1000;
        long start = System.currentTimeMillis();
        int n = 0;
        for (long i = 0; i < durationMillis;) {
            md.await(0.0, Presence.CameraShake, 0.0, 0.0);
            md.stop();
            md.start();
            i = System.currentTimeMillis() - start;
            n++;
        }
        logger.info("tested " + n + " calls to pause/Resume");
    }
}
