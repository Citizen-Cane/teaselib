package teaselib.core.devices.video;

import static org.junit.Assert.*;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_highgui;
import org.junit.Test;

import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;
import teaselib.video.VideoCaptureDevice;

public class VideoCaptureDeviceMultiThreadedTest {
    @Test
    public void testVideoCapture() throws InterruptedException {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);
        VideoCaptureDevice vc = devices.get(VideoCaptureDevice.class).getDefaultDevice();
        DeviceCache.connect(vc);
        assertFalse(vc.active());

        Thread capture = new Thread(() -> capture(vc));
        capture.start();
        Thread.sleep(5000);

        capture.interrupt();
        capture.join();

        vc.close();
    }

    private static void capture(VideoCaptureDevice vc) {
        try (Size size = new Size(640, 480);) {
            vc.open();
            vc.resolution(size);
            vc.fps(30);
            assertEquals(size.width(), vc.resolution().width());
            assertEquals(size.height(), vc.resolution().height());
            for (Mat mat : vc) {
                assertEquals(size.width(), mat.cols());
                assertEquals(size.height(), mat.rows());
                opencv_highgui.imshow("Test", mat);
                if (org.bytedeco.javacpp.opencv_highgui.waitKey(30) >= 0 || Thread.currentThread().isInterrupted()) {
                    break;
                }
            }
        }
    }
}
