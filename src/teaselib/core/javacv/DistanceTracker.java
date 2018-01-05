package teaselib.core.javacv;

import static org.bytedeco.javacpp.opencv_core.FONT_HERSHEY_PLAIN;
import static org.bytedeco.javacpp.opencv_imgproc.putText;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.indexer.FloatIndexer;

public class DistanceTracker {
    private final TrackFeatures tracker = new TrackFeatures();
    private final Mat keyPoints = new Mat();
    private final Scalar color;

    private boolean resetTrackFeatures = false;

    public DistanceTracker(Scalar color) {
        this.color = color;
    }

    public void updateTrackerData(Mat videoImage) {
        tracker.update(videoImage);
    }

    public void update(Mat videoImage, boolean motionDetected) {
        if (motionDetected) {
            resetTrackFeatures = true;
        } else if (resetTrackFeatures) {
            resetDistance(videoImage);
        }
    }

    private void resetDistance(Mat videoImage) {
        tracker.reset(videoImage, null);
        resetTrackFeatures = false;
        tracker.keyPoints().copyTo(keyPoints);
    }

    public void reset() {
        resetTrackFeatures = true;
    }

    public boolean hasFeatures() {
        return tracker.haveFeatures();
    }

    public double distance2() {
        Mat currentKeyPoints = tracker.keyPoints();
        double distance2 = 0.0;
        if (size(currentKeyPoints) > 0) {
            FloatIndexer from = keyPoints.createIndexer();
            FloatIndexer to = currentKeyPoints.createIndexer();
            long n = Math.min(from.rows(), to.rows());
            for (int i = 0; i < n; i++) {
                distance2 = Math.max(distance2,
                        teaselib.core.javacv.util.Geom.distance2(new Point((int) from.get(i, 0), (int) from.get(i, 1)),
                                new Point((int) to.get(i, 0), (int) to.get(i, 1))));
            }
        }
        return distance2;
    }

    public int size(Mat points) {
        return points.rows();
    }

    public void renderDebug(Mat output) {
        Mat currentKeyPoints = tracker.keyPoints();

        FloatIndexer from = keyPoints.createIndexer();
        FloatIndexer to = currentKeyPoints.createIndexer();
        long n = Math.min(from.rows(), to.rows());
        for (int i = 0; i < n; i++) {
            Point p1 = new Point((int) from.get(i, 0), (int) from.get(i, 1));
            Point p2 = new Point((int) to.get(i, 0), (int) to.get(i, 1));
            opencv_imgproc.line(output, p1, p2, color);
            p1.close();
            p2.close();
        }
        int distance = (int) Math.sqrt(distance2());
        Point p = new Point(0, output.rows() - 20);
        putText(output, Integer.toString(distance), p, FONT_HERSHEY_PLAIN, 1.75, color);
        p.close();
        from.release();
        to.release();
    }

    public void render(Mat debugOutput, Scalar color) {
        tracker.render(debugOutput, color);
    }
}
