package teaselib.motiondetection.javacv;

import java.util.Iterator;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;

public interface VideoCaptureDevice extends Iterable<Mat> {

    void open(Size size);

    Size size();

    Size captureSize();

    double fps();

    Iterator<Mat> iterator();

    void close();
}