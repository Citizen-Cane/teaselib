package teaselib.core.javacv;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.indexer.FloatIndexer;

public class DistanceTracker {
    private final Mat keyPoints = new Mat();
    private final Scalar color;

    private boolean resetTrackFeatures = false;

    public DistanceTracker(Scalar color) {
        this.color = color;
    }

    public void update(Mat videoImage, boolean motionDetected,
            TrackFeatures trackFeatures) {
        if (motionDetected) {
            resetTrackFeatures = true;
        } else if (resetTrackFeatures == true) {
            resetDistance(videoImage, trackFeatures);
        }
    }

    private void resetDistance(Mat videoImage, TrackFeatures trackFeatures) {
        trackFeatures.reset(videoImage, null);
        resetTrackFeatures = false;
        trackFeatures.keyPoints().copyTo(keyPoints);
    }

    public void reset() {
        resetTrackFeatures = true;
    }

    public double distance2(Mat currentKeyPoints) {
        double distance2 = 0.0;
        if (size(currentKeyPoints) > 0) {
            FloatIndexer from = keyPoints.createIndexer();
            FloatIndexer to = currentKeyPoints.createIndexer();
            long n = Math.min(from.rows(), to.rows());
            for (int i = 0; i < n; i++) {
                distance2 = Math.max(distance2,
                        teaselib.core.javacv.util.Geom.distance2(
                                new Point((int) from.get(i, 0),
                                        (int) from.get(i, 1)),
                                new Point((int) to.get(i, 0),
                                        (int) to.get(i, 1))));
            }
        }
        return distance2;
    }

    public int size(Mat points) {
        return points.rows();
    }

    public void renderDebug(Mat output, Mat currentKeyPoints) {
        FloatIndexer from = keyPoints.createIndexer();
        FloatIndexer to = currentKeyPoints.createIndexer();
        long n = Math.min(from.rows(), to.rows());
        for (int i = 0; i < n; i++) {
            Point p1 = new Point((int) from.get(i, 0), (int) from.get(i, 1));
            Point p2 = new Point((int) to.get(i, 0), (int) to.get(i, 1));
            opencv_imgproc.line(output, p1, p2, color);
        }
        int distance = (int) Math.sqrt(distance2(currentKeyPoints));
        Point p = new Point(0, output.rows() - 20);
        putText(output, Integer.toString(distance), p, FONT_HERSHEY_PLAIN, 1.75,
                color);
    }
}
