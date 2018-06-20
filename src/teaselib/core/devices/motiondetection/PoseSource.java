package teaselib.core.devices.motiondetection;

import java.util.Arrays;
import java.util.Set;

import org.bytedeco.javacpp.opencv_core.Mat;

import teaselib.core.devices.motiondetection.MotionSource.MotionFunction;
import teaselib.motiondetection.MotionDetector.Presence;
import teaselib.motiondetection.Pose;

/**
 * @author Citizen-Cane
 *
 */
public class PoseSource extends PerceptionSource<Pose> {
    final MotionSource motion;

    public PoseSource(MotionSource motion) {
        this.motion = motion;
    }

    @Override
    public void update(Mat video, long timeStamp) {
        // Ignore
    }

    @Override
    void startNewRecognition() {
        // Ignore
    }

    public boolean await(Pose expected, double timeoutSeconds) {
        if (expected == Pose.Bow) {
            return await(this::bow, timeoutSeconds);
        } else if (expected == Pose.Kneel) {
            return await(this::kneel, timeoutSeconds);
        } else if (expected == Pose.Stand) {
            return await(this::stand, timeoutSeconds);
        } else {
            throw new UnsupportedOperationException(expected.toString());
        }
    }

    private boolean await(MotionFunction expected, double timeoutSeconds) {
        return motion.await(expected, timeoutSeconds);
    }

    boolean stand(MotionDetectionResult result) {
        double timeSpanSeconds = 1.0;
        Set<Presence> presence = result.getPresence(result.getMotionRegion(timeSpanSeconds),
                result.getPresenceRegion(timeSpanSeconds));
        return presence.contains(Presence.Top);
    }

    boolean kneel(MotionDetectionResult result) {
        double timeSpanSeconds = 1.0;
        Set<Presence> presence = result.getPresence(result.getMotionRegion(timeSpanSeconds),
                result.getPresenceRegion(timeSpanSeconds));
        return presence.containsAll(Arrays.asList(Presence.NoTop, Presence.CenterHorizontal));
    }

    boolean bow(MotionDetectionResult result) {
        double timeSpanSeconds = 1.0;
        Set<Presence> presence = result.getPresence(result.getMotionRegion(timeSpanSeconds),
                result.getPresenceRegion(timeSpanSeconds));

        return !presence.contains(Presence.Top) && !presence.contains(Presence.CenterHorizontal)
                && presence.contains(Presence.Bottom);
    }

    @Override
    public Pose get() {
        return current.getAndSet(Pose.Unknown);
    }
}
