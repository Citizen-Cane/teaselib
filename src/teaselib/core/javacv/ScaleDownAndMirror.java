package teaselib.core.javacv;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;

public class ScaleDownAndMirror {
    public final int factor;
    public final Mat mirror;
    public final Mat output;

    @SuppressWarnings("resource")
    public ScaleDownAndMirror(Size from, Size to) {
        this(from, to, new Mat());
    }

    public ScaleDownAndMirror(Size from, Size to, Mat output) {
        this.factor = Math.max(1, from.width() / to.width());
        this.mirror = new Mat();
        this.output = output;
    }

    @SuppressWarnings("resource")
    public Mat update(Mat input) {
        opencv_core.Size size = new opencv_core.Size(
                input.size().width() / factor, input.size().height() / factor);
        resize(input, mirror, size);
        flip(mirror, output, 1);
        return output;
    }
}
