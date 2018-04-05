package teaselib.core.javacv;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.opencv_video.BackgroundSubtractorKNN;

public class BackgroundSubtraction {

    BackgroundSubtractorKNN knn;
    Mat element;
    public Mat output = new Mat();

    final int history;
    final double dist2Threshold;
    final double shadowThreshold;

    public BackgroundSubtraction(int history) {
        this(history, 400, 0.5);
    }

    public BackgroundSubtraction(int history, double dist2Threshold, double shadowThreshold) {
        this.history = history;
        this.dist2Threshold = dist2Threshold;
        this.shadowThreshold = shadowThreshold;
        init(history, dist2Threshold, shadowThreshold);
    }

    public void setStructuringElementSize(int size) {
        try (Size s = new opencv_core.Size(size, size); Point p = new opencv_core.Point(1, 1);) {
            element = opencv_imgproc.getStructuringElement(org.bytedeco.javacpp.opencv_imgproc.MORPH_RECT, s, p);
        }
    }

    private void init(int history, double dist2Threshold, double shadowThreshold) {
        knn = org.bytedeco.javacpp.opencv_video.createBackgroundSubtractorKNN();
        knn.setHistory(history);
        knn.setDetectShadows(true);
        knn.setDist2Threshold(dist2Threshold);
        knn.setShadowThreshold(shadowThreshold);
        knn.setShadowValue(0);
    }

    public void update(Mat input) {
        knn.apply(input, output);
        org.bytedeco.javacpp.opencv_imgproc.morphologyEx(output, output, opencv_imgproc.CV_MOP_OPEN, element);
    }

    public void clear() {
        // clear doesn't clear...
        knn.clear();
        // and reinit causes the next frame to trigger clear() again...
        // init(history, dist2Threshold, shadowThreshold);
    }
}
