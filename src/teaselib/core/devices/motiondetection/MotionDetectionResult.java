/**
 * 
 */
package teaselib.core.devices.motiondetection;

import java.util.Set;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;

import teaselib.core.concurrency.Signal;
import teaselib.motiondetection.MotionDetector.Presence;

/**
 * @author someone
 *
 */
interface MotionDetectionResult {
    boolean updateMotionState(Mat videoImage,
            MotionProcessorJavaCV motionProcessor, long timeStamp);

    boolean awaitChange(Signal signal, final double timeoutSeconds,
            final Presence change);

    Set<Presence> getPresence(Rect motionRegion, Rect presenceRegion);
}