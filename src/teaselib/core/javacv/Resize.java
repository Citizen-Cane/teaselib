package teaselib.core.javacv;

import static org.bytedeco.javacpp.opencv_imgproc.resize;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;

public class Resize {
    public final int factor;

    public Mat output = new Mat();

    public Resize(int resize) {
        this.factor = resize;
    }

    public Mat update(Mat input) {
        resize(input, output, new opencv_core.Size(
                input.size().width() / factor, input.size().height() / factor));
        return output;
    }
}
