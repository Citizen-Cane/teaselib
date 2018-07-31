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

public class TestStandAndBow {
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
            motionDetector.setSensitivity(AssumeSensitivity);
            logger.info("Wait for motion detector to warm up!");
            motionDetector.await(Proximity.Far, 10.0);

            logger.info("Stand!");
            while (!motionDetector.await(Pose.Stand, 15.0)) {
                logger.info("I said 'Stand'!");
            }

            logger.info("Very good. Now kneel!");
            while (!motionDetector.await(Pose.Kneel, 15.0)) {
                System.out.println("I said 'Kneel'!");
            }

            logger.info("Very good. Now Bow and kiss the floor I'm standing on!");
            while (!motionDetector.await(Pose.Kneel, 15.0)) {
                logger.info("I said 'Kneel'!");
            }

            System.out.println("Stay with your nose on the floor!");
            while (!movement.stoppedWithin(1.0, 5.0)) {
                logger.info("I said 'Stay on the floor'!");
            }

            System.out.println("Very good. You may kneel up.");
            while (!motionDetector.await(Pose.Kneel, 15.0)) {
                logger.info("I said 'Kneel up'!");
            }

            System.out.println("Stand up!");
            while (!motionDetector.await(Pose.Stand, 15.0)) {
                logger.info("I said 'Stand up'!");
            }

            logger.info("Did it work!");
            motionDetector.setSensitivity(GestureSensitivity);
            Gesture gesture;
            while (Gesture.None == (gesture = motionDetector.await(Arrays.asList(Gesture.Nod), 5.0))) {
                logger.info("I said 'Did it work?'!");
            }
            if (gesture == Gesture.Shake) {
                continue;
            } else if (gesture == Gesture.Nod) {
                break;
            }
        }

        logger.info("Very good, you're very obedient!");
    }
}
