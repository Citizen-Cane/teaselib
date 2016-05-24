package teaselib.core.javacv;

import static org.bytedeco.javacpp.opencv_imgproc.*;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;

public class ScaleDown {
    public final int factor;
    public final Mat output = new Mat();

    public ScaleDown(int resize) {
        this.factor = resize;
    }

    public Mat update(Mat input) {
        resize(input, output, new opencv_core.Size(
                input.size().width() / factor, input.size().height() / factor));
        return output;
    }
}
