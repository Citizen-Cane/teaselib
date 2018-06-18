package teaselib.core.devices.motiondetection;

import java.util.Set;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;

import teaselib.motiondetection.MotionDetector.Presence;
import teaselib.motiondetection.ViewPoint;

/**
 * @author Citizen-Cane
 *
 */
public interface MotionDetectionResult {
    void setViewPoint(ViewPoint viewPoint);

    boolean updateMotionState(Mat videoImage, MotionProcessorJavaCV motionProcessor, long timeStamp);

    Rect getPresenceRegion(double seconds);

    Rect getMotionRegion(double seconds);

    // public boolean await(Signal signal, double amount, Presence change, double timeSpanSeconds,
    // double timeoutSeconds) throws InterruptedException;

    Set<Presence> getPresence(Rect motionRegion, Rect presenceRegion);
}