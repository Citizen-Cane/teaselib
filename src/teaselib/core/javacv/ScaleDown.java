package teaselib.core.javacv;

import static org.bytedeco.javacpp.opencv_imgproc.*;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;

public class ScaleDown {
    public final int factor;
    public final Mat output;

    @SuppressWarnings("resource")
    public ScaleDown(Size from, Size to) {
        this(from, to, new Mat());
    }

    public ScaleDown(Size from, Size to, Mat output) {
        this.factor = Math.max(1, from.width() / to.width());
        this.output = output;
    }

    public ScaleDown(int resize) {
        this.factor = resize;
        output = new Mat();
    }

    @SuppressWarnings("resource")
    public Mat update(Mat input) {
        opencv_core.Size size = new opencv_core.Size(
                input.size().width() / factor, input.size().height() / factor);
        resize(input, output, size);
        return output;
    }
}
