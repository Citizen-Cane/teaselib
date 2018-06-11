package teaselib.core.javacv;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.indexer.FloatIndexer;

public class DistanceTracker {
    private final TrackFeatures tracker = new TrackFeatures(2);
    private final Mat keyPoints = new Mat();
    private final Scalar color;

    private boolean resetTrackFeatures = false;

    public DistanceTracker(Scalar color) {
        this.color = color;
    }

    public void updateTrackerData(Mat videoImage) {
        tracker.update(videoImage);
    }

    public void updateDistance(Mat videoImage, boolean motionDetected) {
        if (motionDetected) {
            resetTrackFeatures = true;
        } else if (resetTrackFeatures) {
            resetDistance(videoImage);
        }
    }

    private void resetDistance(Mat videoImage) {
        tracker.start(videoImage);
        resetTrackFeatures = false;
        tracker.keyPoints().copyTo(keyPoints);
    }

    public void restart() {
        resetTrackFeatures = true;
    }

    public boolean hasFeatures() {
        return tracker.hasFeatures();
    }

    @SuppressWarnings("resource")
    public double distance2() {
        Mat currentKeyPoints = tracker.keyPoints();
        double distance2 = 0.0;

        if (size(keyPoints) > 0) {
            FloatIndexer from = keyPoints.createIndexer();
            FloatIndexer to = currentKeyPoints.createIndexer();
            long n = Math.min(from.rows(), to.rows());
            for (int i = 0; i < n; i++) {
                distance2 = Math.max(distance2,
                        teaselib.core.javacv.util.Geom.distance2(new Point((int) from.get(i, 0), (int) from.get(i, 1)),
                                new Point((int) to.get(i, 0), (int) to.get(i, 1))));
            }

            from.release();
            to.release();

            try {
                from.close();
            } catch (Exception e) { //
            }
            try {
                to.close();
            } catch (Exception e) { //
            }
        }

        return distance2;
    }

    public int size(Mat points) {
        return points.rows();
    }

    public Mat startPoints() {
        return keyPoints;
    }

    public Mat currentPoints() {
        return tracker.keyPoints();
    }

    public Scalar color() {
        return color;
    }
}
