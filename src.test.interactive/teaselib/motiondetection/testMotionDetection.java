package teaselib.motiondetection;

import org.junit.Test;

import teaselib.TeaseLib;
import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;
import teaselib.motiondetection.MotionDetector.MotionSensitivity;

/**
 * @author someone
 *
 */
public class testMotionDetection {

    /**
     * 
     */
    @Test
    public void test() {
        TeaseLib.init(new DummyHost(), new DummyPersistence());

        MotionDetector md = MotionDetectorFactory.Instance.getDefaultDevice();
        md.setSensitivity(MotionSensitivity.Normal);
        
        System.out.println("Move!");
        while (!md.awaitMotionStart(5)) {
            System.out.println("I said 'Move'!");
        }

        System.out.println("Keep moving!");
        while (true) {
            if (md.awaitMotionEnd(5)) {
                System.out.println("I said 'Keep moving'!");
                // Enjoy the timeout, to avoid repeating too fast
                md.awaitMotionStart(5);
            } else {
                break;
            }
        }

        System.out.println("Stop!");
        while (!md.awaitMotionEnd(5)) {
            System.out.println("I said 'Stop'!");
        }

        System.out.println("Now stay still.");
        // Enjoy the timeout, to avoid repeating too fast
        while (true) {
            if (md.awaitMotionStart(10)) {
                System.out.println("I said 'Stay still'!");
                md.awaitMotionEnd(2);
            } else {
                break;
            }
        }
        System.out.println("Very good.");
    }
}
