package teaselib.motiondetection.javacv;

import java.util.Iterator;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;

public interface VideoCaptureDevice {

    void open(Size size);

    Size size();

    Iterator<Mat> iterator();

    void close();
}