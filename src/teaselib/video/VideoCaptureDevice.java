package teaselib.video;

import java.util.Iterator;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;

import teaselib.core.devices.Device;

public interface VideoCaptureDevice extends Iterable<Mat>, Device {
    public static final Size DefaultResolution = new Size(0, 0);

    public static final double DefaultFPS = 0.0;

    void open();

    void fps(double fps);

    double fps();

    ResolutionList getResolutions();

    void resolution(Size size);

    Size resolution();

    @Override
    Iterator<Mat> iterator();

    @Override
    void close();
}