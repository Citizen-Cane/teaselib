package teaselib.motiondetection;

import java.util.Set;

import teaselib.core.Configuration;
import teaselib.core.VideoRenderer;
import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;
import teaselib.core.devices.motiondetection.MotionDetectorJavaCV;

public abstract class MotionDetector implements Device.Creatable {
    public static synchronized DeviceCache<MotionDetector> getDeviceCache(Devices devices,
            Configuration configuration) {
        return new DeviceCache<MotionDetector>()
                .addFactory(MotionDetectorJavaCV.getDeviceFactory(devices, configuration));
    }

    public static Movement movement(MotionDetector motionDetector) {
        return new Movement(motionDetector);
    }

    public static teaselib.motiondetection.Presence presence(MotionDetector motionDetector) {
        return new teaselib.motiondetection.Presence(motionDetector);
    }

    public enum Feature {
        Motion,
        Presence,
        Posture
    }

    public enum MotionSensitivity {
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

    public enum Presence {
        /**
         * The center of the view, intersection of {@link Presence#CenterHorizontal} and {@link Presence#CenterVertical}
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
         * Calculated from capture input to indicate that there is ongoing motion
         */
        Motion,
        /**
         * Calculated from capture input to indicate that there is no ongoing motion
         */
        NoMotion,
        /**
         * Calculated from capture input to indicate that the user touches the presence region
         */
        Present,
        /**
         * Calculated from capture input to indicate that the user doesn't touch the presence region
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

    public static final double MotionRegionDefaultTimespan = 1.0;
    public static final double PresenceRegionDefaultTimespan = 1.0;

    public abstract void setSensitivity(MotionSensitivity motionSensivity);

    public abstract void setViewPoint(ViewPoint pointOfView);

    public abstract void setVideoRenderer(VideoRenderer videoRenderer);

    public abstract void clearMotionHistory();

    public abstract Set<Feature> getFeatures();

    public abstract boolean awaitChange(double amount, Presence change, double timeSpanSeconds, double timeoutSeconds);

    public abstract void stop();

    public abstract void start();
}
