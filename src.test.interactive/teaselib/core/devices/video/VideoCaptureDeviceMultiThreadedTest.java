package teaselib.core.devices.video;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_highgui;
import org.junit.Test;

import teaselib.core.Configuration;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;
import teaselib.test.DebugSetup;
import teaselib.video.VideoCaptureDevice;

public class VideoCaptureDeviceMultiThreadedTest {
    @Test
    public void testVideoCapture() throws InterruptedException {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);
        VideoCaptureDevice vc = devices.get(VideoCaptureDevice.class).getDefaultDevice();
        DeviceCache.connect(vc);

        Thread capture = new Thread(() -> capture(vc));
        capture.start();
        Thread.sleep(2000);

        capture.interrupt();
        capture.join();

        // Close device to avoid crash on system exit
        vc.close();
    }

    private static void capture(VideoCaptureDevice vc) {
        try (Size size = new Size(320, 240);) {
            vc.fps(30);
            vc.open();
            vc.resolution(size);
            for (Mat mat : vc) {
                opencv_highgui.imshow("Test", mat);
                if (org.bytedeco.javacpp.opencv_highgui.waitKey(30) >= 0 || Thread.currentThread().isInterrupted()) {
                    break;
                }
            }
        }
    }
}
