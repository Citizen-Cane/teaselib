package teaselib.core.javacv;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

import java.util.Iterator;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;

import teaselib.core.javacv.util.Buffer;
import teaselib.core.javacv.util.Buffer.Locked;

public class ScaleAndMirror implements Transformation {
    public final double factor;
    public final Mat mirror;
    public final Iterator<Buffer.Locked<Mat>> output;

    public ScaleAndMirror(Size from, Size to, Buffer<Mat> buffers) {
        this.factor = to.width() / (double) from.width();
        this.mirror = new Mat();
        this.output = buffers.iterator();
    }

    @Override
    public Mat update(Mat input) {
        opencv_core.Size size = new opencv_core.Size((int) (input.size().width() * factor),
                (int) (input.size().height() * factor));
        Locked<Mat> buffer = output.next();
        try {
            Mat mat = buffer.get();
            resize(input, mirror, size);
            flip(mirror, mat, 1);
            return mat;
        } finally {
            buffer.release();
            size.close();
        }
    }
}
