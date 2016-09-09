package teaselib.motiondetection;

import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.motiondetection.MotionDetectorJavaCV;

/**
 * Motion detectors have a 1:1 relationship to video capture devices.
 */
public class MotionDetection {
    public static final DeviceCache<MotionDetector> Devices = new DeviceCache<MotionDetector>()
            .addFactory(MotionDetectorJavaCV.Factory);

    public static Movement movement(MotionDetector motionDetector) {
        return new Movement(motionDetector);
    }

    public static Presence presence(MotionDetector motionDetector) {
        return new Presence(motionDetector);
    }
}
