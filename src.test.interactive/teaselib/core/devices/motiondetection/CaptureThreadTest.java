package teaselib.core.devices.motiondetection;

import org.bytedeco.javacpp.opencv_core.Point;
import org.junit.Test;

import teaselib.core.Configuration;
import teaselib.core.VideoRenderer.Type;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;
import teaselib.core.javacv.VideoRendererJavaCV;
import teaselib.motiondetection.MotionDetector.MotionSensitivity;
import teaselib.motiondetection.ViewPoint;
import teaselib.test.DebugSetup;
import teaselib.video.VideoCaptureDevice;

/**
 * @author Citizen-Cane
 *
 */
public class CaptureThreadTest {
    @Test
    public void testCaptureThread() throws InterruptedException {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);

        VideoCaptureDevice vc = devices.get(VideoCaptureDevice.class).getDefaultDevice();
        DeviceCache.connect(vc);
        MotionDetectorCaptureThread captureThread = new MotionDetectorCaptureThread(vc, 30);
        captureThread.motionSensitivity = MotionSensitivity.Normal;
        captureThread.viewPoint = ViewPoint.EyeLevel;

        captureThread.videoRenderer = new VideoRendererJavaCV(Type.CameraFeedback) {
            @Override
            protected Point getPosition(Type type, int width, int height) {
                return new Point(0, 0);
            }
        };
        captureThread.start();

        captureThread.startCapture();
        Thread.sleep(2000);
        captureThread.stopCapture();

        // to avoid crash on system exit
        // OpenCV/VideoInput Video capture device must be closed
        captureThread.interrupt();
        captureThread.join();

        Thread.sleep(500);
    }
}
