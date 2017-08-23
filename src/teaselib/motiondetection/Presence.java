/**
 * 
 */
package teaselib.motiondetection;

/**
 * @author Citizen-Cane
 *
 */
public class Presence {
    private final MotionDetector motionDetector;

    public Presence(MotionDetector motionDetector) {
        this.motionDetector = motionDetector;
    }

    public boolean isPresentSince(double timeSpanSeconds) {
        return motionDetector.awaitChange(1.0, teaselib.motiondetection.MotionDetector.Presence.Present,
                timeSpanSeconds, 0.0);
    }

    public boolean isAwaySince(double timeSpanSeconds) {
        return motionDetector.awaitChange(1.0, teaselib.motiondetection.MotionDetector.Presence.Present,
                timeSpanSeconds, 0.0);
    }

    public boolean awaitPresence(double presenceTimeSpanSeconds, double timeoutSeconds) {
        return motionDetector.awaitChange(0.8, teaselib.motiondetection.MotionDetector.Presence.Present,
                presenceTimeSpanSeconds, timeoutSeconds);
    }

    public boolean awaitAway(double presenceTimeSpanSeconds, double timeoutSeconds) {
        return motionDetector.awaitChange(0.8, teaselib.motiondetection.MotionDetector.Presence.Away,
                presenceTimeSpanSeconds, timeoutSeconds);
    }
}
