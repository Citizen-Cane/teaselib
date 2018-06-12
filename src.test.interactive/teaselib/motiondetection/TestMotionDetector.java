package teaselib.motiondetection;

import org.bytedeco.javacpp.opencv_core.Point;
import org.junit.Test;

import teaselib.core.Configuration;
import teaselib.core.VideoRenderer.Type;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;
import teaselib.core.javacv.VideoRendererJavaCV;
import teaselib.motiondetection.MotionDetector.MotionSensitivity;
import teaselib.motiondetection.MotionDetector.Presence;
import teaselib.test.DebugSetup;

public class TestMotionDetector {

    @Test
    public void testMotionStartStop() throws InterruptedException {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);

        MotionDetector motionDetector = devices.get(MotionDetector.class).getDefaultDevice();
        motionDetector.setViewPoint(ViewPoint.EyeLevel);

        motionDetector.setVideoRenderer(new VideoRendererJavaCV(Type.CameraFeedback) {
            @Override
            protected Point getPosition(Type type, int width, int height) {
                return new Point(0, 0);
            }
        });
        motionDetector.start();
        // TODO Resolve NPE when sleep is missing (wait for motion & presence data to be initialized)
        DeviceCache.connect(motionDetector);
        Thread.sleep(1000);
        Movement movement = MotionDetector.movement(motionDetector);

        // using an amount < 1.0 ignores small pauses while moving
        // respectively ignores slight motions while standing still
        // Setting amount to 1.0 would make it very difficult to pass the test
        final double amount = 0.95;
        motionDetector.setSensitivity(MotionSensitivity.High);

        System.out.println("Move!");

        while (!motionDetector.await(amount, Presence.Motion, 1.0, 5.0)) {
            System.out.println("I said 'Move'!");
        }

        System.out.println("Keep moving!");
        while (true) {
            // Triggers after not moving for about one second
            if (motionDetector.await(amount, Presence.NoMotion, 1.0, 10.0)) {
                System.out.println("I said 'Keep moving'!");
                // Besides leaving the tester some time to move again
                // the timeout greater than the trigger time span
                // clears the history to avoid being triggered again
                // and repeating the message too fast
                motionDetector.await(amount, Presence.Motion, 1.0, 10.0);
            } else {
                break;
            }
        }

        System.out.println("Stop!");
        motionDetector.setSensitivity(MotionSensitivity.Low);
        while (!motionDetector.await(amount, Presence.NoMotion, 1.0, 5.0)) {
            System.out.println("I said 'Stop'!");
        }

        System.out.println("Now stay still.");
        // use the timeout to avoid repeating the message too fast
        while (true) {
            if (motionDetector.await(amount, Presence.Motion, 0.5, 10.0)) {
                System.out.println("I said 'Stay still'!");
                // Besides leaving the tester some time to stand still
                // the timeout greater than the trigger time span
                // clears the history to avoid being triggered again
                // and repeating the message too fast
                motionDetector.await(amount, Presence.NoMotion, 1.0, 10.0);
            } else {
                break;
            }
        }
        System.out.println("Very good.");
        motionDetector.await(amount, Presence.Motion, 1.0, Double.MAX_VALUE);
    }
}
