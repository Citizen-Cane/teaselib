package teaselib.video;

import java.util.Iterator;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;

import teaselib.core.devices.Device;

public interface VideoCaptureDevice extends Iterable<Mat>, Device {
    public static final Size DefaultResolution = new Size(0, 0);

    void open(Size size);

    Size captureSize();

    double fps();

    @Override
    Iterator<Mat> iterator();

    @Override
    void release();

    ResolutionList getResolutions();

    void setResolution(Size size);
}