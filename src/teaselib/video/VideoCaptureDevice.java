package teaselib.video;

import java.util.Iterator;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;

import teaselib.core.devices.Device;

public interface VideoCaptureDevice extends Iterable<Mat>, Device {

    void open(Size size);

    Size size();

    Size captureSize();

    double fps();

    @Override
    Iterator<Mat> iterator();

    @Override
    void release();
}