package teaselib.motiondetection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.motiondetection.MotionDetectorJavaCV;
import teaselib.video.VideoCaptureDevices;

/**
 * Motion detectors have a 1:1 relationship to video capture devices.
 */
public class MotionDetectorFactory {
    public static final DeviceCache<MotionDetector> Instance = new DeviceCache<MotionDetector>(
            MotionDetectorJavaCV.DeviceClassName,
            new DeviceCache.DeviceFactory<MotionDetector>() {
                @Override
                public List<String> getDevices() {
                    List<String> deviceNames = new ArrayList<String>();
                    Set<String> videoCaptureDevicePaths = VideoCaptureDevices.Instance
                            .getDevicePaths();
                    for (String videoCaptureDevicePath : videoCaptureDevicePaths) {
                        deviceNames.add(DeviceCache.createDevicePath(
                                MotionDetectorJavaCV.DeviceClassName,
                                videoCaptureDevicePath));
                    }
                    return deviceNames;
                }

                @Override
                public MotionDetector getDevice(String devicePath) {
                    return new MotionDetectorJavaCV(VideoCaptureDevices.Instance
                            .getDevice(DeviceCache.getDeviceName(devicePath)));
                }
            }) {
        @Override
        public MotionDetector getDefaultDevice() {
            String defaultId = getLast(getDevicePaths());
            return getDevice(defaultId);
        }
    };
}
