package teaselib.motiondetection;

import java.util.Set;

import teaselib.core.VideoRenderer;
import teaselib.core.devices.Device;

public interface MotionDetector extends Device {

    enum Feature {
        Motion,
        Presence,
        Posture
    }

    enum MotionSensitivity {
        /**
         * Ignores short movements
         */
        Low,

        /**
         * Default sensitivity.
         */
        Normal,

        /**
         * Detects also eye-blinking.
         */
        High
    }

    enum Presence {
        /**
         * The center of the view, intersection of
         * {@link Presence#CenterHorizontal} and {@link Presence#CenterVertical}
         */
        Center,
        /**
         * Region in the middle, from left to right
         */
        CenterHorizontal,

        /**
         * Region in the middle, from top to bottom (minus the borders)
         */
        CenterVertical,
        /**
         * Left region
         */
        Left,
        NoLeft,
        /**
         * Right region
         */
        Right,
        NoRight,
        /**
         * Top region
         */
        Top,
        NoTop,
        /**
         * Bottom region
         */
        Bottom,
        NoBottom,
        /**
         * The left border of the capture view
         */
        LeftBorder,
        NoLeftBorder,
        /**
         * The right border of the capture view
         */
        RightBorder,
        NoRightBorder,
        /**
         * The top border of the capture view
         */
        TopBorder,
        NoTopBorder,

        /**
         * The bottom border of the capture view
         */
        BottomBorder,
        NoBottomBorder,

        /**
         * Calculated from capture input to indicate that there is ongoing
         * motion
         */
        Motion,
        /**
         * Calculated from capture input to indicate that there is no ongoing
         * motion
         */
        NoMotion,
        /**
         * Calculated from capture input to indicate that the user touches the
         * presence region
         */
        Present,
        /**
         * Calculated from capture input to indicate that the user doesn't touch
         * the presence region
         */
        Away,
        /**
         * Calculated from capture input to indicate camera shaking
         */
        Shake,
        NoShake,

        /**
         * The Presence region extended to the top and the bottom.
         */
        PresenceExtendedVertically,
    }

    static final double MotionRegionDefaultTimespan = 1.0;
    static final double PresenceRegionDefaultTimespan = 1.0;

    void setSensitivity(MotionSensitivity motionSensivity);

    void setViewPoint(ViewPoint pointOfView);

    void setVideoRenderer(VideoRenderer videoRenderer);

    void clearMotionHistory();

    Set<Feature> getFeatures();

    public boolean awaitChange(double amount, Presence change,
            double timeSpanSeconds, double timeoutSeconds);

    public void stop();

    public void start();
}
