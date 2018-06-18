package teaselib.core.devices.motiondetection;

import static org.bytedeco.javacpp.opencv_core.FONT_HERSHEY_PLAIN;
import static org.bytedeco.javacpp.opencv_imgproc.putText;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.indexer.FloatIndexer;

import teaselib.core.javacv.BackgroundSubtraction;
import teaselib.core.javacv.Color;
import teaselib.core.javacv.Contours;
import teaselib.core.javacv.DistanceTracker;
import teaselib.core.javacv.TrackFeatures;

public class MotionProcessorJavaCV {
    private static final int HISTORY_SIZE = 1;

    public static final int WARMUP_FRAMES = HISTORY_SIZE + 2;
    public static final Rect None = new Rect(Integer.MAX_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE,
            -Integer.MAX_VALUE);
    public static final Rect Previous = new Rect(-Integer.MAX_VALUE, -Integer.MAX_VALUE, -Integer.MAX_VALUE,
            -Integer.MAX_VALUE);

    int structuringElementSize = 0;
    int distanceThreshold2 = 0;

    BackgroundSubtraction motion;
    Contours motionContours = new Contours();
    DistanceTracker distanceTracker = new DistanceTracker(Color.Green);

    class MotionData {
        final Scalar color = distanceTracker.color();

        final Mat currentPoints = new Mat();
        final Mat startPoints = new Mat();
        double distance2 = 0;

        public void update() {
            distanceTracker.startPoints().copyTo(startPoints);
            distanceTracker.currentPoints().copyTo(currentPoints);
            distance2 = distanceTracker.distance2();
        }
    }

    MotionData motionData = new MotionData();

    MotionProcessorJavaCV() {
        motion = new BackgroundSubtraction(HISTORY_SIZE, 600.0, 0.2);
    }

    /**
     * Calculate the size of the structuring element based on the size of the captured image.
     * 
     * @param nominalSizeAtBaseResolutionWidth
     */
    public void setStructuringElementSize(int size) {
        structuringElementSize = size;
        motion.setStructuringElementSize(structuringElementSize);
        distanceThreshold2 = structuringElementSize * structuringElementSize;
    }

    static int sizeOfStructuringElement(Size captureSize, Size renderSize, int nominalSizeAtBaseResolutionWidth) {
        int captureWidth = captureSize.width();
        int renderWidth = renderSize.width();
        double baseResolutionWidth = 1920;
        double nominalWidthFactor = captureWidth / baseResolutionWidth;
        double sizeFactor = ((double) renderWidth) / ((double) captureWidth) * nominalWidthFactor;
        // The structuring element size must beat least 2
        int finalSize = Math.max(2, (int) (nominalSizeAtBaseResolutionWidth * sizeFactor));
        return finalSize;
    }

    public void update(Mat videoImage) {
        motion.update(videoImage);
        motionContours.update(motion.output);
    }

    public void updateTrackerData(Mat videoImage) {
        if (distanceTracker.hasFeatures()) {
            distanceTracker.updateTrackerData(videoImage);
        }
    }

    public int pixels() {
        return motionContours.pixels();
    }

    public void updateRenderData() {
        motionData.update();
    }

    public static void render(Mat output, MotionData renderData, Scalar color) {
        if (hasFeatures(renderData.startPoints)) {
            try (FloatIndexer from = renderData.startPoints.createIndexer();
                    FloatIndexer to = renderData.currentPoints.createIndexer();) {
                long n = Math.min(from.rows(), to.rows());
                for (int i = 0; i < n; i++) {
                    try (Point p1 = new Point((int) from.get(i, 0), (int) from.get(i, 1));
                            Point p2 = new Point((int) to.get(i, 0), (int) to.get(i, 1));) {
                        opencv_imgproc.line(output, p1, p2, color);
                    }
                }

                from.release();
                to.release();

                int distance = (int) Math.sqrt(renderData.distance2);
                try (Point p = new Point(0, output.rows() - 20);) {
                    putText(output, Integer.toString(distance), p, FONT_HERSHEY_PLAIN, 1.75, color);
                }
            } catch (Exception e) { // Ignored
            }

            TrackFeatures.render(output, color, renderData.currentPoints, null, null);
        }
    }

    private static boolean hasFeatures(Mat points) {
        return points.rows() > 0 && points.cols() > 0;
    }
}
