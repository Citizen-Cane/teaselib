/**
 * 
 */
package teaselib.motiondetection;

import teaselib.motiondetection.MotionDetector.Presence;

/**
 * @author Citizen-Cane
 *
 */
public class Movement {
    /**
     * 
     */
    public static final double DefaultLatency = 0.5;

    private final MotionDetector motionDetector;

    Movement(MotionDetector motionDetector) {
        this.motionDetector = motionDetector;
    }

    public boolean occurring() {
        return occurred(0.0);
    }

    public boolean occurringNot() {
        return occurredNot(0.0);
    }

    public boolean occurred(double timeSpanSeconds) {
        return motionDetector.await(1.0, Presence.Motion, timeSpanSeconds, 0.0);
    }

    public boolean occurredNot(double timeSpanSeconds) {
        return motionDetector.await(1.0, Presence.NoMotion, timeSpanSeconds, 0.0);
    }

    public boolean startedWithin(double timeoutSeconds) {
        return startedWithin(DefaultLatency, timeoutSeconds);
    }

    public boolean startedWithin(double latencySeconds, double timeoutSeconds) {
        return motionDetector.await(1.0, Presence.Motion, latencySeconds, timeoutSeconds);
    }

    public boolean stoppedWithin(double timeoutSeconds) {
        return stoppedWithin(DefaultLatency, timeoutSeconds);
    }

    public boolean stoppedWithin(double latencySeconds, double timeoutSeconds) {
        return motionDetector.await(1.0, Presence.NoMotion, latencySeconds, timeoutSeconds);
    }
}
