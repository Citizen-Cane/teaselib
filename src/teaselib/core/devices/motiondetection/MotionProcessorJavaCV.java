package teaselib.core.devices.motiondetection;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Size;

import teaselib.core.javacv.BackgroundSubtraction;
import teaselib.core.javacv.Color;
import teaselib.core.javacv.Contours;
import teaselib.core.javacv.DistanceTracker;

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
}
