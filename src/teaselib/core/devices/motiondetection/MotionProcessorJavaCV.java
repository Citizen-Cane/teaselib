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
    public static final Rect None = new Rect(Integer.MAX_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE,
            -Integer.MAX_VALUE);
    public static final Rect Previous = new Rect(-Integer.MAX_VALUE, -Integer.MAX_VALUE, -Integer.MAX_VALUE,
            -Integer.MAX_VALUE);

    private final int captureWidth;
    private final int renderWidth;

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

    MotionProcessorJavaCV(Size captureSize, Size renderSize) {
        this.captureWidth = captureSize.width();
        this.renderWidth = renderSize.width();
        motion = new BackgroundSubtraction(1, 600.0, 0.2);
    }

    /**
     * Calculate the size of the structuring element based on the size of the captured image.
     * 
     * @param nominalSizeAtBaseResolutionWidth
     */
    public void setStructuringElementSize(int nominalSizeAtBaseResolutionWidth) {
        double baseResolutionWidth = 1920;
        double nominalWidthFactor = captureWidth / baseResolutionWidth;
        double sizeFactor = ((double) renderWidth) / ((double) captureWidth) * nominalWidthFactor;
        // The structuring element size must beat least 2
        structuringElementSize = Math.max(2, (int) (nominalSizeAtBaseResolutionWidth * sizeFactor));
        motion.setStructuringElementSize(structuringElementSize);
        distanceThreshold2 = structuringElementSize * structuringElementSize;
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
            FloatIndexer from = renderData.startPoints.createIndexer();
            FloatIndexer to = renderData.currentPoints.createIndexer();

            long n = Math.min(from.rows(), to.rows());
            for (int i = 0; i < n; i++) {
                Point p1 = new Point((int) from.get(i, 0), (int) from.get(i, 1));
                Point p2 = new Point((int) to.get(i, 0), (int) to.get(i, 1));
                opencv_imgproc.line(output, p1, p2, color);
                p1.close();
                p2.close();
            }

            int distance = (int) Math.sqrt(renderData.distance2);
            Point p = new Point(0, output.rows() - 20);
            putText(output, Integer.toString(distance), p, FONT_HERSHEY_PLAIN, 1.75, color);
            p.close();

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

            TrackFeatures.render(output, color, renderData.currentPoints);
        }
    }

    private static boolean hasFeatures(Mat points) {
        return points.rows() > 0 && points.cols() > 0;
    }
}
