package teaselib.motiondetection;

import org.junit.Test;

import teaselib.TeaseLib;
import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;
import teaselib.motiondetection.MotionDetector.MotionSensitivity;
import teaselib.motiondetection.MotionDetector.Presence;

public class testMotionDetectionNew {

    @Test
    public void testMotionStartStop() {
        TeaseLib.init(new DummyHost(), new DummyPersistence());

        MotionDetector md = MotionDetectorFactory.Instance.getDefaultDevice();
        md.setSensitivity(MotionSensitivity.Normal);

        System.out.println("Move!");
        while (!md.awaitChange(5, Presence.Motion)) {
            System.out.println("I said 'Move'!");
        }

        System.out.println("Keep moving!");
        while (true) {
            if (md.awaitChange(5, Presence.NoMotion)) {
                System.out.println("I said 'Keep moving'!");
                // use the timeout to avoid repeating the message too fast
                md.awaitChange(5, Presence.Motion);
            } else {
                break;
            }
        }

        System.out.println("Stop!");
        while (!md.awaitChange(5, Presence.NoMotion)) {
            System.out.println("I said 'Stop'!");
        }

        System.out.println("Now stay still.");
        // use the timeout to avoid repeating the message too fast
        while (true) {
            if (md.awaitChange(10, Presence.Motion)) {
                System.out.println("I said 'Stay still'!");
                md.awaitChange(2, Presence.NoMotion);
            } else {
                break;
            }
        }
        System.out.println("Very good.");
        md.awaitChange(Double.MAX_VALUE, Presence.Motion);
    }
}
