package teaselib.core.devices.video;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_highgui;
import org.junit.Test;

import teaselib.video.VideoCaptureDevice;
import teaselib.video.VideoCaptureDevices;

public class VideoCaptureDeviceJavaVCTests {

    @Test
    public void testVideoCapture() {
        VideoCaptureDevice vc = VideoCaptureDevices.Instance.getDefaultDevice();
        @SuppressWarnings("resource")
        Size size = new Size(320, 240);
        vc.open();
        vc.resolution(size);
        for (Mat mat : vc) {
            opencv_highgui.imshow("Test", mat);
            if (org.bytedeco.javacpp.opencv_highgui.waitKey(30) >= 0) {
                break;
            }
        }
        vc.close();
    }
}
