package teaselib.motiondetection.javacv;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_imgproc;

public class Contours {
    public MatVector contours = new MatVector();

    public Contours() {
    }

    public void update(Mat input) {
        opencv_imgproc.findContours(input.clone(), contours,
                opencv_imgproc.RETR_LIST, opencv_imgproc.CHAIN_APPROX_SIMPLE,
                new opencv_core.Point(0, 0));
    }

    public void render(Mat mat, Scalar color, int size) {
        opencv_imgproc.drawContours(mat, contours, size, color);
    }

    public int pixels() {
        int pixels = 0;
        for (int i = 0; i < contours.size(); i++) {
            pixels += opencv_imgproc.contourArea(contours.get(i));
        }
        return pixels;
    }
}
