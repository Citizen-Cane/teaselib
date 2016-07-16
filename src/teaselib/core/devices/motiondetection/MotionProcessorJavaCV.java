package teaselib.core.devices.motiondetection;

import static teaselib.core.javacv.Color.Green;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Size;

import teaselib.core.javacv.BackgroundSubtraction;
import teaselib.core.javacv.Contours;
import teaselib.core.javacv.DistanceTracker;
import teaselib.core.javacv.TrackFeatures;

public class MotionProcessorJavaCV {
    public final static Rect None = new Rect(Integer.MAX_VALUE,
            Integer.MAX_VALUE, -Integer.MAX_VALUE, -Integer.MAX_VALUE);
    public final static Rect Previous = new Rect(-Integer.MAX_VALUE,
            -Integer.MAX_VALUE, -Integer.MAX_VALUE, -Integer.MAX_VALUE);

    private final int captureWidth;
    private final int renderWidth;

    public int structuringElementSize = 0;
    public int distanceThreshold2 = 0;

    BackgroundSubtraction motion;
    Contours motionContours = new Contours();
    TrackFeatures trackFeatures = new TrackFeatures();
    DistanceTracker distanceTracker = new DistanceTracker(Green);

    /**
     * @param captureSize
     * @param renderSize
     */
    MotionProcessorJavaCV(Size captureSize, Size renderSize) {
        this.captureWidth = captureSize.width();
        this.renderWidth = renderSize.width();
        motion = new BackgroundSubtraction(1, 600.0, 0.2);
    }

    /**
     * Calculate the size of the structuring element based on the size of the
     * captured image.
     * 
     * @param nominalSizeAtBaseResolutionWidth
     */
    public void setStructuringElementSize(
            int nominalSizeAtBaseResolutionWidth) {
        double baseResolutionWidth = 1920;
        double nominalWidthFactor = captureWidth / baseResolutionWidth;
        double sizeFactor = ((double) renderWidth) / ((double) captureWidth)
                * nominalWidthFactor;
        // The structuring element size must beat least 2
        structuringElementSize = Math.max(2,
                (int) (nominalSizeAtBaseResolutionWidth * sizeFactor));
        motion.setStructuringElementSize(structuringElementSize);
        distanceThreshold2 = structuringElementSize * structuringElementSize;
    }

    public void update(Mat input) {
        motion.update(input);
        motionContours.update(motion.output);
        if (trackFeatures.haveFeatures()) {
            trackFeatures.update(input);
        }
    }

    public int pixels() {
        return motionContours.pixels();
    }
}
