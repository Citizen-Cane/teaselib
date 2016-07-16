package teaselib.core.javacv;

import static org.bytedeco.javacpp.opencv_core.flip;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;

public class ScaleAndMirror implements Transformation {
    public final double factor;
    public final Mat mirror;
    public final Mat output;

    @SuppressWarnings("resource")
    public ScaleAndMirror(Size from, Size to) {
        this(from, to, new Mat());
    }

    public ScaleAndMirror(Size from, Size to, Mat output) {
        this.factor = to.width() / (double) from.width();
        this.mirror = new Mat();
        this.output = output;
    }

    @Override
    @SuppressWarnings("resource")
    public Mat update(Mat input) {
        opencv_core.Size size = new opencv_core.Size(
                (int) (input.size().width() * factor),
                (int) (input.size().height() * factor));
        resize(input, mirror, size);
        flip(mirror, output, 1);
        return output;
    }
}
