package teaselib.core.javacv;

import static org.bytedeco.javacpp.opencv_imgproc.*;

import java.util.Iterator;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;

import teaselib.core.javacv.util.Buffer;
import teaselib.core.javacv.util.Buffer.Locked;

public class Scale implements Transformation {
    public final double factor;
    public final Iterator<Buffer.Locked<Mat>> output;

    public Scale(Size from, Size to, Buffer<Mat> output) {
        this.factor = factor(from, to);
        this.output = output.iterator();
    }

    public Scale(int factor, Buffer<Mat> buffers) {
        this.factor = factor;
        this.output = buffers.iterator();
    }

    public static double factor(Size from, Size to) {
        return to.width() / (double) from.width();
    }

    @Override
    public Mat update(Mat input) {
        opencv_core.Size size = new opencv_core.Size((int) (input.size().width() * factor),
                (int) (input.size().height() * factor));
        Locked<Mat> buffer = output.next();
        try {
            Mat mat = buffer.get();
            resize(input, mat, size);
            return mat;
        } finally {
            buffer.release();
            size.close();
        }
    }
}
