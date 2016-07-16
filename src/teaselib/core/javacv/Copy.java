package teaselib.core.javacv;

import org.bytedeco.javacpp.opencv_core.Mat;

public class Copy implements Transformation {
    public final Mat output;

    public Copy(Mat output) {
        this.output = output;
    }

    public Copy() {
        output = new Mat();
    }

    @Override
    public Mat update(Mat input) {
        input.copyTo(output);
        return output;
    }
}
