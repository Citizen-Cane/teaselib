package teaselib.motiondetection;

import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.motiondetection.MotionDetectorJavaCV;

/**
 * Motion detectors have a 1:1 relationship to video capture devices.
 */
public class MotionDetection {

    public static final DeviceCache<MotionDetector> Instance = new DeviceCache<MotionDetector>() {
        @Override
        public MotionDetector getDefaultDevice() {
            String defaultId = getLast(getDevicePaths());
            return getDevice(defaultId);
        }
    }.addFactory(MotionDetectorJavaCV.Factory);

    public static Movement movement(MotionDetector motionDetector) {
        return new Movement(motionDetector);
    }

    public static Presence presence(MotionDetector motionDetector) {
        return new Presence(motionDetector);
    }
}
