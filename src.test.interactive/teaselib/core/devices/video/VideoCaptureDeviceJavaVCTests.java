package teaselib.core.devices.video;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_highgui;
import org.junit.Test;

import teaselib.core.Configuration;
import teaselib.core.devices.Devices;
import teaselib.test.DebugSetup;
import teaselib.video.VideoCaptureDevice;

public class VideoCaptureDeviceJavaVCTests {

    @Test
    public void testVideoCapture() {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);

        VideoCaptureDevice vc = devices.get(VideoCaptureDevice.class).getDefaultDevice();
        Size size = new Size(320, 240);
        vc.open();
        vc.resolution(size);
        for (Mat mat : vc) {
            opencv_highgui.imshow("Test", mat);
            if (org.bytedeco.javacpp.opencv_highgui.waitKey(30) >= 0) {
                break;
            }
        }
        size.close();
        vc.close();
    }
}
