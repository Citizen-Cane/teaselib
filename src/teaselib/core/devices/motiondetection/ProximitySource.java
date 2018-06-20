package teaselib.core.devices.motiondetection;

import java.util.Arrays;
import java.util.Set;

import org.bytedeco.javacpp.opencv_core.Mat;

import teaselib.core.devices.motiondetection.MotionSource.MotionFunction;
import teaselib.motiondetection.MotionDetector.Presence;
import teaselib.motiondetection.Proximity;

/**
 * @author Citizen-Cane
 *
 */
public class ProximitySource extends PerceptionSource<Proximity> {
    final MotionSource motion;

    public ProximitySource(MotionSource motion) {
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

    public boolean await(Proximity expected, double timeoutSeconds) {
        if (expected == Proximity.Close) {
            return await(this::arriveClose, timeoutSeconds);
        } else if (expected == Proximity.Near) {
            return await(this::arriveNear, timeoutSeconds);
        } else if (expected == Proximity.Far) {
            return await(this::arriveFar, timeoutSeconds);
        } else if (expected == Proximity.FarOrAway) {
            return await(this::arriveFar, timeoutSeconds);
        } else if (expected == Proximity.Away) {
            return await(this::arriveFar, timeoutSeconds);
        } else {
            throw new UnsupportedOperationException(expected.toString());
        }
    }

    private boolean await(MotionFunction expected, double timeoutSeconds) {
        return motion.await(expected, timeoutSeconds);
    }

    boolean arriveClose(MotionDetectionResult result) {
        double timeSpanSeconds = 1.0;
        Set<Presence> presence = result.getPresence(result.getMotionRegion(timeSpanSeconds),
                result.getPresenceRegion(timeSpanSeconds));
        return presence.containsAll(Arrays.asList(Presence.Top, Presence.Bottom, Presence.Right, Presence.Left));
    }

    boolean arriveNear(MotionDetectionResult result) {
        double timeSpanSeconds = 1.0;
        Set<Presence> presence = result.getPresence(result.getMotionRegion(timeSpanSeconds),
                result.getPresenceRegion(timeSpanSeconds));
        return presence.contains(Presence.CameraShake) || result.getPresenceRegion(timeSpanSeconds)
                .width() > motion.presenceResult.presenceIndicators.get(Presence.Center).width();
    }

    boolean arriveFar(MotionDetectionResult result) {
        double timeSpanSeconds = 1.0;
        Set<Presence> presence = result.getPresence(result.getMotionRegion(timeSpanSeconds),
                result.getPresenceRegion(timeSpanSeconds));
        if (presence.contains(Presence.CameraShake) || presence.contains(Presence.NoMotion)) {
            return false;
        }

        return result.getPresenceRegion(timeSpanSeconds).width() < motion.presenceResult.presenceIndicators
                .get(Presence.Center).width();
    }

    boolean arriveFarOrAway(MotionDetectionResult result) {
        double timeSpanSeconds = 1.0;
        Set<Presence> presence = result.getPresence(result.getMotionRegion(timeSpanSeconds),
                result.getPresenceRegion(timeSpanSeconds));

        if (presence.contains(Presence.Away)) {
            return true;
        }

        if (presence.contains(Presence.CameraShake) || presence.contains(Presence.NoMotion)) {
            return false;
        }

        return result.getPresenceRegion(timeSpanSeconds).width() < motion.presenceResult.presenceIndicators
                .get(Presence.Center).width();
    }

    boolean arriveAway(MotionDetectionResult result) {
        double timeSpanSeconds = 1.0;
        Set<Presence> presence = result.getPresence(result.getMotionRegion(timeSpanSeconds),
                result.getPresenceRegion(timeSpanSeconds));
        return presence.contains(Presence.Away);
    }

    @Override
    public Proximity get() {
        return current.getAndSet(Proximity.Unknown);
    }
}
