package teaselib.motiondetection;

import java.util.ArrayList;
import java.util.List;

import teaselib.motiondetection.javacv.VideoCaptureDevice;
import teaselib.motiondetection.javacv.VideoCaptureDeviceCV;

public class VideoCaptureDeviceFactory {
    public static final DeviceCache<VideoCaptureDevice> Instance = new DeviceCache<>(
            VideoCaptureDeviceCV.DeviceClassName,
            new DeviceCache.DeviceFactory<VideoCaptureDevice>() {

                @Override
                public List<String> getDevices() {
                    List<String> deviceNames = new ArrayList<>();
                    deviceNames.addAll(VideoCaptureDeviceCV.getDevicesPaths());
                    return deviceNames;
                }

                @Override
                public VideoCaptureDevice getDevice(String devicePath) {
                    return VideoCaptureDeviceCV
                            .get(DeviceCache.getDeviceName(devicePath));
                }
            });
}
