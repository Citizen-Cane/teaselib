package teaselib.core.javacv;

import static teaselib.core.javacv.util.Geom.join;
import static teaselib.core.javacv.util.Gui.rectangles;

import java.util.List;
import java.util.Vector;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;

public class MotionProcessor {
    BackgroundSubtraction motion;
    Contours motionContours = new Contours();

    public final static Rect None = new Rect(Integer.MAX_VALUE,
            Integer.MAX_VALUE, -Integer.MAX_VALUE, -Integer.MAX_VALUE);
    public final static Rect Previous = new Rect(-Integer.MAX_VALUE,
            -Integer.MAX_VALUE, -Integer.MAX_VALUE, -Integer.MAX_VALUE);

    private static final int motionQueueCapacity = 5;
    private final List<Rect> motionQueue = new Vector<>(motionQueueCapacity);

    private final int captureWidth;
    private final int renderWidth;

    /**
     * @param captureWidth
     * @param renderWidth
     */
    MotionProcessor(int captureWidth, int renderWidth) {
        this.captureWidth = captureWidth;
        this.renderWidth = renderWidth;
        motion = new BackgroundSubtraction(1, 600.0, 0.2);
        motionQueue.add(None);
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
        int structuringElementSize = Math.max(2,
                (int) (nominalSizeAtBaseResolutionWidth * sizeFactor));
        motion.setStructuringElementSize(structuringElementSize);
    }

    public void update(Mat input) {
        motion.update(input);
        motionContours.update(motion.output);
    }

    public Rect region() {
        List<Rect> rectangles = rectangles(motionContours.contours);
        if (rectangles.size() == 0) {
            // motionRect = None; // Previous;
        } else {
            if (motionQueue.size() == motionQueueCapacity) {
                motionQueue.remove(0);
            }
            // TODO partition and throw out small groups
            // when we have at least two large ones
            // -> eliminate those below let's say 1/4 mean value
            motionQueue.add(join(rectangles));
        }
        return join(motionQueue);
    }

    public Rect motionRect() {
        return motionQueue.get(motionQueue.size() - 1);
    }

    public int pixels() {
        return motionContours.pixels();
    }

}
