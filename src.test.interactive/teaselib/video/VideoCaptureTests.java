package teaselib.video;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_highgui;
import org.junit.Test;

import teaselib.TeaseLib;
import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;

public class VideoCaptureTests {

    @Test
    public void testVideoCapture() {
        TeaseLib.init(new DummyHost(), new DummyPersistence());
        VideoCaptureDevice vc = VideoCaptureDeviceFactory.Instance
                .getDefaultDevice();
        vc.open(new Size(320, 240));
        for (Mat mat : vc) {
            opencv_highgui.imshow("Test", mat);
            if (org.bytedeco.javacpp.opencv_highgui.waitKey(30) >= 0) {
                break;
            }
        }
        vc.release();
    }
}
