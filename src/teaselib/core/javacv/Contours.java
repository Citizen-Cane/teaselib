package teaselib.core.javacv;

import static teaselib.core.javacv.util.Geom.*;

import java.util.List;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_imgproc;

public class Contours {
    private static final Point Null = new opencv_core.Point(0, 0);

    final public MatVector contours = new MatVector();

    public Contours() {
    }

    public void update(Mat input) {
        opencv_imgproc.findContours(input.clone(), contours,
                opencv_imgproc.RETR_LIST, opencv_imgproc.CHAIN_APPROX_SIMPLE,
                Null);
    }

    public void render(Mat mat, int index, Scalar color) {
        opencv_imgproc.drawContours(mat, contours, index, color);
    }

    public int pixels() {
        int pixels = 0;
        for (int i = 0; i < contours.size(); i++) {
            pixels += opencv_imgproc.contourArea(contours.get(i));
        }
        return pixels;
    }

    public List<Rect> regions() {
        return rectangles(contours);
    }
}
