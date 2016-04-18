package teaselib.core.devices;

import java.util.Iterator;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;

import teaselib.motiondetection.Device;

public interface VideoCaptureDevice extends Iterable<Mat>, Device {

    void open(Size size);

    Size size();

    Size captureSize();

    double fps();

    Iterator<Mat> iterator();

    void close();
}