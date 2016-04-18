package teaselib.motiondetection;

import java.util.EnumSet;

import teaselib.core.devices.Device;

public interface MotionDetector extends Device {

    enum Feature {
        Motion,
        Presence,
        Posture
    }

    enum MotionSensitivity {
        Low,
        Normal,
        High
    }

    enum Presence {
        Shake,
        Absent,
        Present,
        Left,
        Right,
        Top,
        Bottom
    }

    void setSensitivity(MotionSensitivity motionSensivity);

    void clearMotionHistory();

    boolean isMotionDetected(double pastSeconds);

    EnumSet<Presence> getPresence();

    EnumSet<Feature> getFeatures();

    /**
     * Waits the specified period for motion.
     * 
     * @param timeoutSeconds
     * @return True if motion started within the time period. False if no motion
     *         was detected during the time period.
     */
    boolean awaitMotionStart(double timeoutSeconds);

    /**
     * @param timeoutSeconds
     * @return True if motion stopped within the time period. False if motion is
     *         still detected at the end of the time period.
     */
    boolean awaitMotionEnd(double timeoutSeconds);

    /**
     * Webcam connected and processing is enabled
     * 
     * @return
     */
    boolean active();

}