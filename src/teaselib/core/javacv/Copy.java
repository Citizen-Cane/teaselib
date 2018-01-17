package teaselib.core.javacv;

import java.util.Iterator;

import org.bytedeco.javacpp.opencv_core.Mat;

import teaselib.core.javacv.util.Buffer;
import teaselib.core.javacv.util.Buffer.Locked;

public class Copy implements Transformation {
    public final Iterator<Buffer.Locked<Mat>> output;

    public Copy(Buffer<Mat> buffers) {
        this.output = buffers.iterator();
    }

    @Override
    public Mat update(Mat input) {
        Locked<Mat> buffer = output.next();
        try {
            Mat mat = buffer.get();
            input.copyTo(mat);
            return mat;
        } finally {
            buffer.release();
        }
    }

}
