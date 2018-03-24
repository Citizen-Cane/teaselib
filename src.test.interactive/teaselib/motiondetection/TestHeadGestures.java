package teaselib.motiondetection;

import java.util.Arrays;

import org.bytedeco.javacpp.opencv_core.Point;
import org.junit.Test;

import teaselib.core.Configuration;
import teaselib.core.VideoRenderer.Type;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;
import teaselib.core.javacv.VideoRendererJavaCV;
import teaselib.motiondetection.MotionDetector.MotionSensitivity;
import teaselib.test.DebugSetup;

public class TestHeadGestures {
    MotionSensitivity AssumeSensitivity = MotionSensitivity.Low;
    MotionSensitivity GestureSensitivity = MotionSensitivity.High;

    @Test
    public void testHeadGestures() throws InterruptedException {
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

        System.out.println("Assume the position!");

        motionDetector.setSensitivity(AssumeSensitivity);
        while (!movement.stoppedWithin(1.0, 5.0)) {
            System.out.println("Freeze'!");
        }

        System.out.println("Nod!");

        motionDetector.setSensitivity(GestureSensitivity);
        while (Gesture.Nod != motionDetector.await(Arrays.asList(Gesture.Nod), 5.0)) {
            System.out.println("I said 'Nod!'");
        }

        System.out.println("Very good! Now stay put until my next command");

        motionDetector.setSensitivity(AssumeSensitivity);
        while (!movement.stoppedWithin(1.0, 5.0)) {
            System.out.println("Freeze'!");
        }

        System.out.println("Shake!");

        motionDetector.setSensitivity(GestureSensitivity);
        while (Gesture.Shake != motionDetector.await(Arrays.asList(Gesture.Shake), 5.0)) {
            System.out.println("I said 'Shake!'");
        }

        System.out.println("Very good, you're very obedient!");
    }
}
