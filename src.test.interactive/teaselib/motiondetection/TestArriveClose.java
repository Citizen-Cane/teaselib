package teaselib.motiondetection;

import java.util.Arrays;

import org.bytedeco.javacpp.opencv_core.Point;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.VideoRenderer.Type;
import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;
import teaselib.core.javacv.VideoRendererJavaCV;
import teaselib.motiondetection.MotionDetector.MotionSensitivity;

public class TestArriveClose {
    static final Logger logger = LoggerFactory.getLogger(TestArriveClose.class);

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

            logger.info("Did it work!");
            motionDetector.setSensitivity(GestureSensitivity);
            Gesture gesture = Gesture.None;
            while (Gesture.None == (gesture = motionDetector.await(Arrays.asList(Gesture.Nod), 5.0))) {
                logger.info("I said 'Did it work?'!");
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

        logger.info("Wait for motion detector to warm up!");
        motionDetector.await(Proximity.Far, 10.0);

        logger.info("Come close!");
        // while (!mdJV.await(amount, Presence.CameraShake, 0.2, 5.0)) {
        while (!motionDetector.await(Proximity.Close, 5.0)) {
            logger.info("I said 'Come close'!");
        }

        System.out.println("Very good. Now step back!");
        // while (!mdJV.await(amount, Presence.NoTopBorder, 3.0, 5.0)) {
        while (!motionDetector.await(Proximity.Far, 5.0)) {
            logger.info("I said 'Step back'!");
        }

        logger.info("Stand still!");
        while (!movement.stoppedWithin(1.0, 5.0)) {
            logger.info("I said 'Stand still'!");
        }
    }
}
