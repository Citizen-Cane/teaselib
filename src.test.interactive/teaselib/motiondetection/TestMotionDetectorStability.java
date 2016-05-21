/**
 * 
 */
package teaselib.motiondetection;

import org.junit.BeforeClass;
import org.junit.Test;

import teaselib.TeaseLib;
import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;
import teaselib.motiondetection.MotionDetector.MotionSensitivity;
import teaselib.motiondetection.MotionDetector.Presence;

/**
 * @author someone
 *
 */
public class TestMotionDetectorStability {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Test
    public void testPauseResume() {
        TeaseLib.init(new DummyHost(), new DummyPersistence());

        MotionDetector md = MotionDetectorFactory.Instance.getDefaultDevice();
        md.setSensitivity(MotionSensitivity.Normal);

        int durationMillis = 10 * 1000;
        long start = System.currentTimeMillis();
        int n = 0;
        for (long i = 0; i < durationMillis;) {
            md.awaitChange(0.0, Presence.Shake, 0.0, 0.0);
            md.pause();
            md.resume();
            i = System.currentTimeMillis() - start;
            n++;
        }
        TeaseLib.instance().log.info("tested " + n + " calls to pause/Resume");
    }
}
