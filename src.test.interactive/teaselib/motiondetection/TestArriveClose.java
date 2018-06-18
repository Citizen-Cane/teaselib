package teaselib.motiondetection;

import java.util.Arrays;

import org.bytedeco.javacpp.opencv_core.Point;
import org.junit.Test;

import teaselib.core.Configuration;
import teaselib.core.VideoRenderer.Type;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;
import teaselib.core.devices.motiondetection.MotionDetectorJavaCV;
import teaselib.core.javacv.VideoRendererJavaCV;
import teaselib.motiondetection.MotionDetector.MotionSensitivity;
import teaselib.test.DebugSetup;

public class TestArriveClose {
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

        while (true) {
            arriveCloseAndStepBack(motionDetector, movement);

            System.out.println("Did it work!");
            motionDetector.setSensitivity(GestureSensitivity);
            Gesture gesture = Gesture.None;
            while (Gesture.None == (gesture = motionDetector.await(Arrays.asList(Gesture.Nod), 5.0))) {
                System.out.println("I said 'Did it work?'!");
            }
            if (gesture == Gesture.Shake) {
                continue;
            } else if (gesture == Gesture.Nod) {
                break;
            }
        }

        System.out.println("Very good, you're very obedient!");
    }

    private void arriveCloseAndStepBack(MotionDetector motionDetector, Movement movement) {
        motionDetector.setSensitivity(AssumeSensitivity);
        // double amount = 0.95;
        MotionDetectorJavaCV mdJV = (MotionDetectorJavaCV) motionDetector;

        System.out.println("Wait for motion detector to warm up!");
        mdJV.await(Proximity.Far, 10.0);

        System.out.println("Come close!");
        // while (!mdJV.await(amount, Presence.CameraShake, 0.2, 5.0)) {
        while (!mdJV.await(Proximity.Close, 5.0)) {
            System.out.println("I said 'Come close'!");
        }

        System.out.println("Very good. Now step back!");
        // while (!mdJV.await(amount, Presence.NoTopBorder, 3.0, 5.0)) {
        while (!mdJV.await(Proximity.Far, 5.0)) {
            System.out.println("I said 'Step back'!");
        }

        System.out.println("Stand still!");
        while (!movement.stoppedWithin(1.0, 5.0)) {
            System.out.println("I said 'Stand still'!");
        }
    }
}
