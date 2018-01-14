package teaselib.motiondetection;

import org.bytedeco.javacpp.opencv_core.Point;
import org.junit.Test;

import teaselib.core.Configuration;
import teaselib.core.VideoRenderer.Type;
import teaselib.core.devices.Devices;
import teaselib.core.javacv.VideoRendererJavaCV;
import teaselib.motiondetection.MotionDetector.MotionSensitivity;
import teaselib.test.DebugSetup;

public class TestHeadGestures {

    @Test
    public void testHeadGestures() {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);

        MotionDetector motionDetector = devices.get(MotionDetector.class).getDefaultDevice();
        motionDetector.setViewPoint(ViewPoint.EyeLevel);
        motionDetector.setSensitivity(MotionSensitivity.High);

        motionDetector.setVideoRenderer(new VideoRendererJavaCV(Type.CameraFeedback) {
            @Override
            protected Point getPosition(Type type, int width, int height) {
                return new Point(0, 0);
            }
        });
        Movement movement = MotionDetector.movement(motionDetector);

        System.out.println("Assume the position!");

        while (!movement.stoppedWithin(1.0, 5.0)) {
            System.out.println("Freeze'!");
        }

        System.out.println("Nod!");

        while (Gesture.Nod != motionDetector.await(Gesture.Nod, 5.0)) {
            System.out.println("I said 'Nod!'");
        }

        System.out.println("Very good! Now stay put until my next command");

        while (!movement.stoppedWithin(1.0, 5.0)) {
            System.out.println("Freeze'!");
        }

        System.out.println("Shake!");

        while (Gesture.Shake != motionDetector.await(Gesture.Shake, 5.0)) {
            System.out.println("I said 'Shake!'");
        }

        System.out.println("Very good, you're very obedient!");
    }
}
