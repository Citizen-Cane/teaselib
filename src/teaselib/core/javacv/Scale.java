package teaselib.core.javacv;

import static org.bytedeco.javacpp.opencv_imgproc.resize;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;

public class Scale implements Transformation {
    public final double factor;
    public final Mat output;

    @SuppressWarnings("resource")
    public Scale(Size from, Size to) {
        this(from, to, new Mat());
    }

    public Scale(Size from, Size to, Mat output) {
        this.factor = to.width() / (double) from.width();
        this.output = output;
    }

    public Scale(int resize) {
        this.factor = resize;
        output = new Mat();
    }

    /*
     * (non-Javadoc)
     * 
     * @see teaselib.core.javacv.MatTransformer#update(org.bytedeco.javacpp.
     * opencv_core.Mat)
     */
    @Override
    @SuppressWarnings("resource")
    public Mat update(Mat input) {
        opencv_core.Size size = new opencv_core.Size(
                (int) (input.size().width() * factor),
                (int) (input.size().height() * factor));
        resize(input, output, size);
        return output;
    }
}
