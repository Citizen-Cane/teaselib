package teaselib.core.javacv;

import org.bytedeco.javacpp.opencv_core.Mat;

public interface Transformation {

    Mat update(Mat input);

}