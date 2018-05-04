package teaselib.motiondetection;

import org.bytedeco.javacpp.opencv_core.Point;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Configuration;
import teaselib.core.VideoRenderer.Type;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;
import teaselib.core.javacv.VideoRendererJavaCV;
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
    public void testPauseResume() throws InterruptedException {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);

        MotionDetector motionDetector = devices.get(MotionDetector.class).getDefaultDevice();
        motionDetector.setSensitivity(MotionSensitivity.Normal);
        motionDetector.setViewPoint(ViewPoint.EyeLevel);

        motionDetector.setVideoRenderer(new VideoRendererJavaCV(Type.CameraFeedback) {
            @Override
            protected Point getPosition(Type type, int width, int height) {
                return new Point(0, 0);
            }
        });

        DeviceCache.connect(motionDetector);

        int durationMillis = 10 * 1000;
        long start = System.currentTimeMillis();
        int n = 0;

        // TODO Resolve NPE when sleep is missing (wait for motion & presence data to be initialized)
        motionDetector.start();
        Thread.sleep(2000);

        for (long i = 0; i < durationMillis;) {
            motionDetector.await(0.0, Presence.CameraShake, 0.0, 0.0);
            motionDetector.stop();
            motionDetector.start();
            i = System.currentTimeMillis() - start;
            n++;
        }
        logger.info("tested " + n + " calls to pause/Resume");
    }
}
