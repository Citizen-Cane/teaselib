package teaselib.core.devices.motiondetection;

import java.util.Set;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;

import teaselib.core.concurrency.Signal;
import teaselib.motiondetection.MotionDetector.Presence;
import teaselib.motiondetection.ViewPoint;

/**
 * @author someone
 *
 */
interface MotionDetectionResult {
    void setViewPoint(ViewPoint viewPoint);

    boolean updateMotionState(Mat videoImage,
            MotionProcessorJavaCV motionProcessor, long timeStamp);

    Rect getPresenceRegion(double seconds);

    Rect getMotionRegion(double seconds);

    public boolean await(Signal signal, final double amount,
            final Presence change, final double timeSpanSeconds,
            final double timeoutSeconds) throws InterruptedException;

    Set<Presence> getPresence(Rect motionRegion, Rect presenceRegion);
}