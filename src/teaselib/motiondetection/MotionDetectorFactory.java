package teaselib.motiondetection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import teaselib.core.devices.VideoCaptureDeviceFactory;
import teaselib.core.javacv.MotionDetectorJavaCV;

/**
 * Motion detectors have a 1:1 relationship to video capture devices.
 */
public class MotionDetectorFactory {
    public static final DeviceCache<MotionDetector> Instance = new DeviceCache<>(
            MotionDetectorJavaCV.DeviceClassName,
            new DeviceCache.DeviceFactory<MotionDetector>() {
                @Override
                public List<String> getDevices() {
                    List<String> deviceNames = new ArrayList<>();
                    Set<String> videoCaptureDevicePaths = VideoCaptureDeviceFactory.Instance
                            .getDevices();
                    for (String videoCaptureDevicePath : videoCaptureDevicePaths) {
                        deviceNames.add(DeviceCache.createDevicePath(
                                MotionDetectorJavaCV.DeviceClassName,
                                videoCaptureDevicePath));
                    }
                    return deviceNames;
                }

                @Override
                public MotionDetector getDevice(String devicePath) {
                    return new MotionDetectorJavaCV(
                            VideoCaptureDeviceFactory.Instance.getDevice(
                                    DeviceCache.getDeviceName(devicePath)));
                }
            });
}
